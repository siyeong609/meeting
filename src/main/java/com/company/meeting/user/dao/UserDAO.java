package com.company.meeting.user.dao;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.user.dto.UserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * user 테이블 접근 전용 DAO
 * - SQL만 담당
 * - Connection은 외부에서 주입하지 않고 내부에서 관리
 */
public class UserDAO {

    /**
     * 로그인 ID로 사용자 조회
     */
    public UserDTO findByLoginId(String loginId) {

        String sql = """
                    SELECT id, login_id, password, name, role, created_at
                    FROM user
                    WHERE login_id = ?
                """;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, loginId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                UserDTO user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setLoginId(rs.getString("login_id"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                return user;
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("User 조회 실패", e);
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException ignored) {
            }
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException ignored) {
            }
            DBConnection.close(conn);
        }
    }

    /**
     * 전체 회원 수 조회 (관리자 제외하고 싶으면 조건 추가 가능)
     */
    public int countAllUsers() {

        String sql = "SELECT COUNT(*) FROM user";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
