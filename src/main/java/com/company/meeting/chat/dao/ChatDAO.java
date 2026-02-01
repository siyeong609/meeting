package com.company.meeting.chat.dao;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.chat.dto.ChatMessageItem;
import com.company.meeting.chat.dto.ChatThreadDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ChatDAO
 * - 너가 준 스키마(chat_thread, chat_message)에 1:1 문의 MVP 로직을 맞춤
 *
 * 주의:
 * - user 테이블을 JOIN 해서 senderName/senderLoginId를 채워줌
 * - user는 예약어일 수 있으니 `user` 백틱 사용
 */
public class ChatDAO {

    /**
     * ✅ 회원당 1개 thread 보장
     * - 없으면 생성(OPEN)
     * - 동시성(중복 INSERT) 발생 가능 → UNIQUE 충돌 시 재조회로 처리
     */
    public ChatThreadDTO ensureThread(int userId) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            ChatThreadDTO t = findThreadByUserId(conn, userId);
            if (t != null) return t;

            // 없으면 생성
            try {
                int newId = insertThread(conn, userId);
                return new ChatThreadDTO(newId, userId, "OPEN");
            } catch (SQLException e) {
                // ✅ UNIQUE 충돌 등으로 insert 실패 시 다시 조회
                ChatThreadDTO retry = findThreadByUserId(conn, userId);
                if (retry != null) return retry;
                throw e;
            }
        }
    }

    private ChatThreadDTO findThreadByUserId(Connection conn, int userId) throws Exception {
        String sql = "SELECT id, user_id, status FROM chat_thread WHERE user_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ChatThreadDTO(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("status")
                );
            }
        }
    }

    private int insertThread(Connection conn, int userId) throws Exception {
        String sql = "INSERT INTO chat_thread(user_id, status, last_message_at) VALUES(?, 'OPEN', NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new IllegalStateException("thread 생성 실패(키 없음)");
                return rs.getInt(1);
            }
        }
    }

    /**
     * ✅ 메시지 목록 조회
     * - sinceId==0: 최근 limit개 DESC 조회 후 reverse하여 ASC로 반환
     * - sinceId>0 : sinceId 이후만 ASC로 반환
     */
    public List<ChatMessageItem> listMessages(int threadId, long sinceId, int limit) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            if (sinceId <= 0) {
                String sql =
                        "SELECT " +
                                "  m.id AS id, " +
                                "  m.sender_role AS senderRole, " +
                                "  COALESCE(u.name, '관리자') AS senderName, " +
                                "  COALESCE(u.login_id, 'ADMIN') AS senderLoginId, " +
                                "  m.content AS content, " +
                                "  DATE_FORMAT(m.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt " +
                                "FROM chat_message m " +
                                "LEFT JOIN `user` u ON u.id = m.sender_id " +
                                "WHERE m.thread_id = ? " +
                                "ORDER BY m.id DESC " +
                                "LIMIT ?";

                List<ChatMessageItem> desc = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, threadId);
                    ps.setInt(2, limit);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            desc.add(mapMessage(rs));
                        }
                    }
                }
                Collections.reverse(desc);
                return desc;
            }

            String sql =
                    "SELECT " +
                            "  m.id AS id, " +
                            "  m.sender_role AS senderRole, " +
                            "  COALESCE(u.name, '관리자') AS senderName, " +
                            "  COALESCE(u.login_id, 'ADMIN') AS senderLoginId, " +
                            "  m.content AS content, " +
                            "  DATE_FORMAT(m.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt " +
                            "FROM chat_message m " +
                            "LEFT JOIN `user` u ON u.id = m.sender_id " +
                            "WHERE m.thread_id = ? AND m.id > ? " +
                            "ORDER BY m.id ASC " +
                            "LIMIT ?";

            List<ChatMessageItem> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, threadId);
                ps.setLong(2, sinceId);
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(mapMessage(rs));
                    }
                }
            }
            return out;
        }
    }

    /**
     * ✅ USER 메시지 저장
     * - chat_message insert
     * - chat_thread.last_message_at 업데이트
     * - 저장한 메시지 1건을 JOIN 포함해서 반환
     */
    public ChatMessageItem insertUserMessage(int threadId, int userId, String content) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            long newId;

            String insertSql =
                    "INSERT INTO chat_message(thread_id, sender_role, sender_id, content) " +
                            "VALUES(?, 'USER', ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, threadId);
                ps.setInt(2, userId);
                ps.setString(3, content);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new IllegalStateException("메시지 생성 실패(키 없음)");
                    newId = rs.getLong(1);
                }
            }

            // ✅ thread last_message_at 업데이트
            String updSql = "UPDATE chat_thread SET last_message_at = NOW() WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                ps.setInt(1, threadId);
                ps.executeUpdate();
            }

            // ✅ 저장된 메시지 다시 조회(메타 포함)
            String selectSql =
                    "SELECT " +
                            "  m.id AS id, " +
                            "  m.sender_role AS senderRole, " +
                            "  COALESCE(u.name, '나') AS senderName, " +
                            "  COALESCE(u.login_id, '-') AS senderLoginId, " +
                            "  m.content AS content, " +
                            "  DATE_FORMAT(m.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt " +
                            "FROM chat_message m " +
                            "LEFT JOIN `user` u ON u.id = m.sender_id " +
                            "WHERE m.id = ? LIMIT 1";

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setLong(1, newId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("저장된 메시지 조회 실패");
                    return mapMessage(rs);
                }
            }
        }
    }

    private ChatMessageItem mapMessage(ResultSet rs) throws Exception {
        return new ChatMessageItem(
                rs.getLong("id"),
                rs.getString("senderRole"),
                rs.getString("senderName"),
                rs.getString("senderLoginId"),
                rs.getString("content"),
                rs.getString("createdAt")
        );
    }
}
