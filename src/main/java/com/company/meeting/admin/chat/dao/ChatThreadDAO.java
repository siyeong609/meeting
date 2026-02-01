package com.company.meeting.admin.chat.dao;

import com.company.meeting.admin.chat.dto.AdminChatThreadListItem;
import com.company.meeting.common.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatThreadDAO
 * - 관리자 스레드(문의) 목록 조회 전용 DAO
 *
 * 핵심 변경:
 * - 최근 메시지 시간(last_message_at) 뿐 아니라,
 *   최근 메시지 내용(content)도 같이 조회해서 목록에 뿌린다.
 *
 * 성능 포인트:
 * - 너가 추가한 인덱스 idx_chat_message_thread_id_id(thread_id, id) 덕분에
 *   "thread_id별 최신 1건" 서브쿼리가 빠르게 동작한다(페이지당 limit 규모).
 */
public class ChatThreadDAO {

    // ✅ 화면 표시 포맷 (프로젝트 공통 유틸 있으면 거기로 이동 권장)
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 스레드 총 개수(검색/상태 필터 적용)
     */
    public int countThreads(String q, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) ");
        sql.append(buildFromWhere(q, status));

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            idx = bindWhereParams(ps, idx, q, status);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 스레드 목록 조회 (페이징)
     *
     * 정렬:
     * - 최근 메시지(last_message_at)가 있는 스레드를 위로
     * - last_message_at이 NULL(메시지 없음)은 뒤로
     * - 그 다음 updated_at 기준 보조정렬
     */
    public List<AdminChatThreadListItem> selectThreads(int offset, int limit, String q, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  t.id AS thread_id, ");
        sql.append("  t.user_id AS user_id, ");
        sql.append("  ").append(userLoginSelect()).append(" AS user_login_id, ");
        sql.append("  t.status AS status, ");
        sql.append("  t.last_message_at AS last_message_at, ");
        sql.append("  t.created_at AS created_at, ");

        // ✅ 최근 메시지 content 1건 조회 (thread_id별 최신 id 기준)
        // - idx(thread_id, id) 덕분에 ORDER BY id DESC LIMIT 1이 효율적
        sql.append("  (");
        sql.append("    SELECT m.content ");
        sql.append("    FROM chat_message m ");
        sql.append("    WHERE m.thread_id = t.id ");
        sql.append("    ORDER BY m.id DESC ");
        sql.append("    LIMIT 1 ");
        sql.append("  ) AS last_message_content ");

        sql.append(buildFromWhere(q, status));
        sql.append(" ORDER BY (t.last_message_at IS NULL) ASC, t.last_message_at DESC, t.updated_at DESC ");
        sql.append(" LIMIT ? OFFSET ? ");

        List<AdminChatThreadListItem> out = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            idx = bindWhereParams(ps, idx, q, status);

            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int threadId = rs.getInt("thread_id");
                    int userId = rs.getInt("user_id");
                    String userLoginId = rs.getString("user_login_id");
                    String st = rs.getString("status");

                    Timestamp lastTs = rs.getTimestamp("last_message_at");
                    Timestamp createdTs = rs.getTimestamp("created_at");

                    String lastMessageAt = (lastTs == null) ? "-" : formatTs(lastTs);
                    String createdAt = (createdTs == null) ? "-" : formatTs(createdTs);

                    // ✅ 최근 메시지 프리뷰(없으면 "-")
                    String lastContent = rs.getString("last_message_content");
                    String lastPreview = buildOneLinePreview(lastContent);

                    out.add(new AdminChatThreadListItem(
                            threadId,
                            userId,
                            userLoginId,
                            st,
                            lastPreview,
                            lastMessageAt,
                            createdAt
                    ));
                }
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------
    // 내부 헬퍼
    // -------------------------

    /**
     * FROM/WHERE 절 구성
     *
     * ✅ user 테이블명이 `user` 라면 MySQL 예약어 충돌 가능성이 있어 백틱 사용
     * ✅ 로그인ID 컬럼명은 userLoginSelect()에서 프로젝트에 맞춰 조정
     */
    private String buildFromWhere(String q, String status) {
        StringBuilder sb = new StringBuilder();

        sb.append(" FROM chat_thread t ");
        sb.append(" JOIN `user` u ON u.id = t.user_id ");
        sb.append(" WHERE 1=1 ");

        if (q != null && !q.isEmpty()) {
            // 괄호로 묶어서 OR 결합 시 우선순위 꼬임 방지
            sb.append(" AND ( ");
            sb.append(userLoginWhereLike());
            if (isNumeric(q)) {
                sb.append(" OR t.user_id = ? ");
            }
            sb.append(" ) ");
        }

        if (status != null && !status.isEmpty()) {
            sb.append(" AND t.status = ? ");
        }

        return sb.toString();
    }

    private int bindWhereParams(PreparedStatement ps, int startIdx, String q, String status) throws Exception {
        int idx = startIdx;

        if (q != null && !q.isEmpty()) {
            ps.setString(idx++, "%" + q + "%");
            if (isNumeric(q)) {
                ps.setInt(idx++, Integer.parseInt(q));
            }
        }

        if (status != null && !status.isEmpty()) {
            ps.setString(idx++, status);
        }

        return idx;
    }

    private static boolean isNumeric(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static String formatTs(Timestamp ts) {
        LocalDateTime ldt = ts.toLocalDateTime();
        return DT.format(ldt);
    }

    /**
     * ✅ 최근 메시지 프리뷰는 "1줄"로만 만든다.
     * - 줄바꿈/탭/연속 공백을 공백 1개로 압축
     * - 내용이 없으면 "-"
     *
     * 실제 ... 처리(ellipsis)는 CSS에서 처리하는 게 깔끔함.
     */
    private static String buildOneLinePreview(String content) {
        if (content == null) return "-";
        String t = content.trim();
        if (t.isEmpty()) return "-";

        // 공백/개행/탭 등을 1칸으로 정규화
        t = t.replaceAll("\\s+", " ");

        // 너무 긴 경우를 대비해 서버에서도 1차 컷(옵션)
        // CSS ellipsis가 있지만, DB에서 아주 긴 TEXT면 JSON도 커지므로 적당히 자르는게 안전
        int max = 200;
        if (t.length() > max) {
            t = t.substring(0, max);
        }

        return t;
    }

    /**
     * ✅ 여기만 프로젝트 user 테이블 컬럼명에 맞춰 조정하면 됨
     *
     * 1) 로그인 아이디 컬럼이 login_id 라면:
     *    return "u.login_id";
     *
     * 2) 로그인 아이디 컬럼이 id 라면(로그인ID가 id):
     *    return "u.id";
     */
    private String userLoginSelect() {
        return "u.login_id"; // <-- 필요 시 u.id 로 변경
    }

    private String userLoginWhereLike() {
        return userLoginSelect() + " LIKE ? ";
    }
}
