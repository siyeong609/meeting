package com.company.meeting.admin.chat.dao;

import com.company.meeting.admin.chat.dto.AdminChatMessageItem;
import com.company.meeting.common.db.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminChatMessageDAO
 * - 관리자 채팅 팝업에서 필요한 메시지 조회/전송 DB 처리
 */
public class AdminChatMessageDAO {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * threadId에 대해 sinceId보다 큰 메시지들을 조회 (오름차순)
     */
    public List<AdminChatMessageItem> selectMessages(int threadId, long sinceId, int limit) {
        String sql =
                "SELECT id, thread_id, sender_role, sender_id, content, created_at " +
                        "FROM chat_message " +
                        "WHERE thread_id = ? AND id > ? " +
                        "ORDER BY id ASC " +
                        "LIMIT ?";

        List<AdminChatMessageItem> out = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, threadId);
            ps.setLong(2, sinceId);
            ps.setInt(3, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    int tid = rs.getInt("thread_id");
                    String role = rs.getString("sender_role");

                    int senderIdVal = rs.getInt("sender_id");
                    Integer senderId = rs.wasNull() ? null : senderIdVal;

                    String content = rs.getString("content");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    String createdAt = createdTs == null ? "-" : DT.format(createdTs.toLocalDateTime());

                    out.add(new AdminChatMessageItem(id, tid, role, senderId, content, createdAt));
                }
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 관리자 메시지 1건 삽입
     * - insert 후 생성된 id 포함하여 반환
     */
    public AdminChatMessageItem insertAdminMessage(int threadId, Integer adminIdOrNull, String content) {
        String insertSql =
                "INSERT INTO chat_message(thread_id, sender_role, sender_id, content) " +
                        "VALUES (?, 'ADMIN', ?, ?)";

        String selectSql =
                "SELECT id, thread_id, sender_role, sender_id, content, created_at " +
                        "FROM chat_message WHERE id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            long newId;

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, threadId);

                // ✅ sender_id는 null 허용
                if (adminIdOrNull == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, adminIdOrNull);

                ps.setString(3, content);

                int affected = ps.executeUpdate();
                if (affected != 1) {
                    conn.rollback();
                    throw new RuntimeException("메시지 저장에 실패했습니다.");
                }

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        throw new RuntimeException("생성된 메시지 ID를 가져오지 못했습니다.");
                    }
                    newId = keys.getLong(1);
                }
            }

            // ✅ thread last_message_at 갱신
            updateThreadLastMessageAt(conn, threadId);

            // ✅ 저장된 행 다시 조회(표준 포맷으로 반환)
            AdminChatMessageItem item;
            try (PreparedStatement ps2 = conn.prepareStatement(selectSql)) {
                ps2.setLong(1, newId);
                try (ResultSet rs = ps2.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new RuntimeException("저장된 메시지를 조회하지 못했습니다.");
                    }

                    long id = rs.getLong("id");
                    int tid = rs.getInt("thread_id");
                    String role = rs.getString("sender_role");

                    int senderIdVal = rs.getInt("sender_id");
                    Integer senderId = rs.wasNull() ? null : senderIdVal;

                    String c = rs.getString("content");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    String createdAt = createdTs == null ? "-" : DT.format(createdTs.toLocalDateTime());

                    item = new AdminChatMessageItem(id, tid, role, senderId, c, createdAt);
                }
            }

            conn.commit();
            return item;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * thread 존재 여부 확인
     */
    public boolean existsThread(int threadId) {
        String sql = "SELECT 1 FROM chat_thread WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threadId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 트랜잭션 내부에서 last_message_at 갱신
     */
    private void updateThreadLastMessageAt(Connection conn, int threadId) throws SQLException {
        String sql = "UPDATE chat_thread SET last_message_at = NOW() WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, threadId);
            ps.executeUpdate();
        }
    }
}
