package com.company.meeting.user.dao;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.user.dto.UserDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * user 테이블 접근 전용 DAO
 * - SQL만 담당
 * - Connection은 내부에서 관리
 */
public class UserDAO {

    // ✅ 디폴트 프로필 경로(DB 저장값)
    private static final String DEFAULT_PROFILE_DB_PATH =
            "/resources/uploads/profile/default-profile.svg";

    /**
     * 로그인 ID로 사용자 조회
     */
    public UserDTO findByLoginId(String loginId) {

        String sql = """
                SELECT id, login_id, password, name, email, profile_image, role, memo, created_at, updated_at
                FROM user
                WHERE login_id = ?
                """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) return null;

                UserDTO user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setLoginId(rs.getString("login_id"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));
                user.setProfileImage(rs.getString("profile_image"));
                user.setRole(rs.getString("role"));
                user.setMemo(rs.getString("memo"));

                Timestamp c = rs.getTimestamp("created_at");
                if (c != null) user.setCreatedAt(c.toLocalDateTime());

                Timestamp u = rs.getTimestamp("updated_at");
                if (u != null) user.setUpdatedAt(u.toLocalDateTime());

                return user;
            }

        } catch (SQLException e) {
            throw new RuntimeException("User 조회 실패", e);
        }
    }

    /**
     * 전체 회원 수 조회(대시보드 용)
     * - ADMIN 포함
     */
    public int countAllUsers() {
        String sql = "SELECT COUNT(*) FROM user";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * (페이징/검색) 유저 수 카운트
     * - ADMIN 포함
     */
    public long countUsersByQuery(String q) {

        boolean hasQuery = q != null && !q.isBlank();
        String sql;

        if (!hasQuery) {
            sql = "SELECT COUNT(*) FROM user";
        } else {
            sql = """
                    SELECT COUNT(*)
                    FROM user
                    WHERE login_id LIKE ? OR name LIKE ? OR email LIKE ?
                    """;
        }

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            if (hasQuery) {
                String like = "%" + q + "%";
                pstmt.setString(1, like);
                pstmt.setString(2, like);
                pstmt.setString(3, like);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("User count 조회 실패", e);
        }

        return 0;
    }

    /**
     * (페이징/검색) 유저 목록 조회
     * - ADMIN 포함
     * - password는 목록에서 제외
     */
    public List<UserDTO> findUsersByQuery(String q, int offset, int size) {

        boolean hasQuery = q != null && !q.isBlank();

        String sql;
        if (!hasQuery) {
            sql = """
                    SELECT id, login_id, name, email, profile_image, role, memo, created_at, updated_at
                    FROM user
                    ORDER BY created_at DESC, id DESC
                    LIMIT ? OFFSET ?
                    """;
        } else {
            sql = """
                    SELECT id, login_id, name, email, profile_image, role, memo, created_at, updated_at
                    FROM user
                    WHERE login_id LIKE ? OR name LIKE ? OR email LIKE ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT ? OFFSET ?
                    """;
        }

        List<UserDTO> users = new ArrayList<>();

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            int idx = 1;

            if (hasQuery) {
                String like = "%" + q + "%";
                pstmt.setString(idx++, like);
                pstmt.setString(idx++, like);
                pstmt.setString(idx++, like);
            }

            pstmt.setInt(idx++, size);
            pstmt.setInt(idx, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserDTO u = new UserDTO();
                    u.setId(rs.getInt("id"));
                    u.setLoginId(rs.getString("login_id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    u.setProfileImage(rs.getString("profile_image"));
                    u.setRole(rs.getString("role"));
                    u.setMemo(rs.getString("memo"));

                    Timestamp c = rs.getTimestamp("created_at");
                    if (c != null) u.setCreatedAt(c.toLocalDateTime());

                    Timestamp up = rs.getTimestamp("updated_at");
                    if (up != null) u.setUpdatedAt(up.toLocalDateTime());

                    users.add(u);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("User 목록 조회 실패", e);
        }

        return users;
    }

    /**
     * ✅ 회원 생성(기존 시그니처 유지)
     * - memo는 미포함(호환용)
     * - 내부적으로 memo 포함 메서드를 호출하도록 변경
     */
    public int insertUser(String loginId, String passwordHash, String name, String email, String role) {
        // ✅ 기존 호출부 호환 유지: memo는 null로 처리
        return insertUser(loginId, passwordHash, name, email, role, null);
    }

    /**
     * ✅ 회원 생성 (memo 포함 버전)
     * - profile_image는 기본값으로 저장
     * - memo는 빈 문자열이면 NULL 처리
     * - 반환값: 생성된 PK(id)
     */
    public int insertUser(String loginId, String passwordHash, String name, String email, String role, String memo) {

        String sql = """
                INSERT INTO user (login_id, password, name, email, profile_image, role, memo)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            pstmt.setString(1, loginId);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, name);

            // ✅ email optional (blank -> NULL)
            pstmt.setString(4, (email == null || email.isBlank()) ? null : email);

            // ✅ 디폴트는 NULL 저장
            pstmt.setNull(5, Types.VARCHAR);

            // ✅ role 저장 (서비스에서 default USER 처리 권장)
            pstmt.setString(6, role);

            // ✅ memo optional (blank -> NULL)
            if (memo == null || memo.isBlank()) {
                pstmt.setNull(7, Types.LONGVARCHAR);
            } else {
                pstmt.setString(7, memo);
            }

            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("회원 생성 실패", e);
        }

        return 0;
    }

    /**
     * ✅ 회원 단건 삭제
     * - role=USER만 삭제 허용(ADMIN 보호)
     * - 성공하면 1, 실패/대상없음이면 0
     */
    public int deleteUserById(int userId) {

        String sql = "DELETE FROM user WHERE id = ? AND role = 'USER'";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("회원 삭제 실패(userId=" + userId + ")", e);
        }
    }

    public void updatePasswordHash(int userId, String hashedPassword) {
        String sql = "UPDATE user SET password = ? WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Password 업데이트 실패", e);
        }
    }

    public void updateEmail(int userId, String email) {
        String sql = "UPDATE user SET email = ? WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, (email == null || email.isBlank()) ? null : email);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("이메일 업데이트 실패", e);
        }
    }

    public void updateProfileImage(int userId, String profileImagePath) {
        String sql = "UPDATE user SET profile_image = ? WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, profileImagePath);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("프로필 이미지 업데이트 실패", e);
        }
    }

    public void clearProfileImage(int userId) {
        String sql = "UPDATE user SET profile_image = NULL WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("프로필 이미지 삭제(Null) 실패", e);
        }
    }

    public String findProfileImagePath(int userId) {
        String sql = "SELECT profile_image FROM user WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException("프로필 이미지 경로 조회 실패", e);
        }

        return null;
    }

    /**
     * ✅ 관리자 메모 업데이트(빈 문자열이면 NULL 처리)
     */
    public void updateMemo(int userId, String memo) {
        String sql = "UPDATE user SET memo = ? WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            if (memo == null || memo.isBlank()) {
                pstmt.setNull(1, Types.LONGVARCHAR);
            } else {
                pstmt.setString(1, memo);
            }
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("메모 업데이트 실패", e);
        }
    }
}
