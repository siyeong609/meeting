package com.company.meeting.reservation.dao;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.reservation.dto.ReservationListItem;
import com.company.meeting.reservation.dto.ReservationRoomPolicy;
import com.company.meeting.reservation.dto.RoomDayOperating;
import com.company.meeting.reservation.dto.RoomReservationItem;
import com.company.meeting.reservation.dto.RoomReservationDayCount;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ReservationDAO
 * - reservation CRUD + 충돌 체크 + room 정책/운영시간 조회
 *
 * 충돌 체크 핵심:
 * - 동일 room_id, status=BOOKED
 * - 시간이 겹치면 실패
 * - buffer_minutes 반영을 위해 (start-buffer) ~ (end+buffer)로 확장해 판단
 */
public class ReservationDAO {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public int countMyReservations(int userId, String q) throws SQLException {
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = ""
                + "SELECT COUNT(*) "
                + "FROM reservation r "
                + "JOIN room rm ON rm.id = r.room_id "
                + "WHERE r.user_id = ? "
                + (hasQ ? "AND (rm.name LIKE ? OR IFNULL(r.title,'') LIKE ?) " : "");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, userId);

            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<ReservationListItem> findMyReservations(int userId, String q, int offset, int size) throws SQLException {
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = ""
                + "SELECT r.id, r.room_id, rm.name AS room_name, rm.location AS room_location, "
                + "       r.title, r.status, r.start_time, r.end_time, r.created_at "
                + "FROM reservation r "
                + "JOIN room rm ON rm.id = r.room_id "
                + "WHERE r.user_id = ? "
                + (hasQ ? "AND (rm.name LIKE ? OR IFNULL(r.title,'') LIKE ?) " : "")
                + "ORDER BY r.start_time DESC "
                + "LIMIT ? OFFSET ?";

        List<ReservationListItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, userId);

            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }

            ps.setInt(idx++, size);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReservationListItem it = new ReservationListItem();
                    it.setId(rs.getInt("id"));
                    it.setRoomId(rs.getInt("room_id"));
                    it.setRoomName(rs.getString("room_name"));
                    it.setRoomLocation(rs.getString("room_location"));
                    it.setTitle(rs.getString("title"));
                    it.setStatus(rs.getString("status"));

                    Timestamp st = rs.getTimestamp("start_time");
                    Timestamp et = rs.getTimestamp("end_time");
                    Timestamp ct = rs.getTimestamp("created_at");

                    it.setStartTime(st == null ? "" : st.toLocalDateTime().format(DT_FMT));
                    it.setEndTime(et == null ? "" : et.toLocalDateTime().format(DT_FMT));
                    it.setCreatedAt(ct == null ? "" : ct.toLocalDateTime().format(DT_FMT));

                    list.add(it);
                }
            }
        }

        return list;
    }

    /**
     * room 정책 조회 (예약 검증용)
     */
    public ReservationRoomPolicy findRoomPolicy(int roomId) throws SQLException {
        String sql = ""
                + "SELECT is_active, available_start_date, available_end_date, "
                + "       slot_minutes, min_minutes, max_minutes, buffer_minutes, booking_open_days_ahead "
                + "FROM room WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                ReservationRoomPolicy p = new ReservationRoomPolicy();
                p.setActive(rs.getInt("is_active") == 1);

                Date s = rs.getDate("available_start_date");
                Date e = rs.getDate("available_end_date");
                p.setAvailableStartDate(s == null ? null : s.toString());
                p.setAvailableEndDate(e == null ? null : e.toString());

                p.setSlotMinutes(rs.getInt("slot_minutes"));
                p.setMinMinutes(rs.getInt("min_minutes"));
                p.setMaxMinutes(rs.getInt("max_minutes"));
                p.setBufferMinutes(rs.getInt("buffer_minutes"));
                p.setBookingOpenDaysAhead(rs.getInt("booking_open_days_ahead"));

                return p;
            }
        }
    }

    /**
     * 특정 날짜 운영시간 조회
     * - 예외 테이블 우선
     * - 없으면 주간 운영시간(dow) 사용
     */
    public RoomDayOperating findOperatingForDate(int roomId, LocalDate date) throws SQLException {
        // 1) exception 우선
        String sqlEx = ""
                + "SELECT is_closed, open_time, close_time, reason "
                + "FROM room_operating_exceptions "
                + "WHERE room_id = ? AND exception_date = ?";

        // 2) weekly fallback
        String sqlWeekly = ""
                + "SELECT is_closed, open_time, close_time "
                + "FROM room_operating_hours "
                + "WHERE room_id = ? AND dow = ?";

        try (Connection conn = DBConnection.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(sqlEx)) {
                ps.setInt(1, roomId);
                ps.setDate(2, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean closed = rs.getInt("is_closed") == 1;
                        Time ot = rs.getTime("open_time");
                        Time ct = rs.getTime("close_time");

                        String open = (ot == null) ? null : ot.toLocalTime().format(TIME_FMT);
                        String close = (ct == null) ? null : ct.toLocalTime().format(TIME_FMT);
                        String reason = rs.getString("reason");

                        return new RoomDayOperating(closed, open, close, reason);
                    }
                }
            }

            int dow = date.getDayOfWeek().getValue(); // 1=월..7=일

            try (PreparedStatement ps = conn.prepareStatement(sqlWeekly)) {
                ps.setInt(1, roomId);
                ps.setInt(2, dow);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return new RoomDayOperating(true, null, null, null);
                    }

                    boolean closed = rs.getInt("is_closed") == 1;
                    Time ot = rs.getTime("open_time");
                    Time ct = rs.getTime("close_time");

                    String open = (ot == null) ? null : ot.toLocalTime().format(TIME_FMT);
                    String close = (ct == null) ? null : ct.toLocalTime().format(TIME_FMT);

                    return new RoomDayOperating(closed, open, close, null);
                }
            }
        }
    }

    /**
     * 예약 생성(충돌 체크 포함)
     * - 트랜잭션 안에서 "겹치는 예약 row"를 잠금으로 확인
     *
     * ✅ MySQL 문법 주의:
     * - LIMIT이 FOR UPDATE 보다 먼저 와야 함
     */
    public int insertReservationWithConflictCheck(
            int userId, int roomId, String title,
            LocalDateTime start, LocalDateTime end,
            int bufferMinutes
    ) throws SQLException {

        // ✅ 방어: 음수 버퍼 방지
        final int buf = Math.max(0, bufferMinutes);

        // ✅ 초/나노 제거
        final LocalDateTime startNorm = normalizeToMinute(start);
        final LocalDateTime endNorm = normalizeToMinute(end);

        if (startNorm == null || endNorm == null) {
            throw new SQLException("예약 시간이 올바르지 않습니다.");
        }
        if (!endNorm.isAfter(startNorm)) {
            throw new SQLException("종료시간은 시작시간보다 커야 합니다.");
        }

        // ✅ buffer 반영 범위
        final LocalDateTime startWithBuffer = startNorm.minusMinutes(buf);
        final LocalDateTime endWithBuffer = endNorm.plusMinutes(buf);

        final String sqlLock = ""
                + "SELECT id, start_time, end_time "
                + "FROM reservation "
                + "WHERE room_id = ? AND status = 'BOOKED' "
                + "  AND start_time < ? "
                + "  AND end_time > ? "
                + "ORDER BY start_time ASC "
                + "LIMIT 1 FOR UPDATE";

        final String sqlInsert = ""
                + "INSERT INTO reservation (user_id, room_id, title, status, start_time, end_time) "
                + "VALUES (?, ?, ?, 'BOOKED', ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1) 충돌 체크(+잠금)
                try (PreparedStatement ps = conn.prepareStatement(sqlLock)) {
                    ps.setInt(1, roomId);
                    ps.setTimestamp(2, Timestamp.valueOf(endWithBuffer));
                    ps.setTimestamp(3, Timestamp.valueOf(startWithBuffer));

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int conflictId = rs.getInt("id");
                            Timestamp cst = rs.getTimestamp("start_time");
                            Timestamp cet = rs.getTimestamp("end_time");

                            // ✅ 어떤 예약과 충돌인지 메시지에 박아버리면 즉시 원인 파악 가능
                            throw new SQLException("이미 해당 시간에 예약이 존재합니다. (conflictId="
                                    + conflictId + ", " + cst + " ~ " + cet + ")");
                        }
                    }
                }

                // 2) insert
                int newId;
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, roomId);
                    ps.setString(3, (title == null) ? null : title.trim());
                    ps.setTimestamp(4, Timestamp.valueOf(startNorm));
                    ps.setTimestamp(5, Timestamp.valueOf(endNorm));

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("예약 ID를 가져오지 못했습니다.");
                        newId = keys.getInt(1);
                    }
                }

                conn.commit();
                return newId;

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private LocalDateTime normalizeToMinute(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    }

    /**
     * ✅ (추가) 특정 회의실의 특정 일자 예약 목록(BOOKED)
     * - 타임테이블 렌더링용
     */
    public List<RoomReservationItem> findRoomReservationsForDate(int roomId, LocalDate date) throws SQLException {
        String sql = ""
                + "SELECT id, title, start_time, end_time "
                + "FROM reservation "
                + "WHERE room_id = ? "
                + "  AND status = 'BOOKED' "
                + "  AND start_time >= ? "
                + "  AND start_time < ? "
                + "ORDER BY start_time ASC";

        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<RoomReservationItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomReservationItem it = new RoomReservationItem();
                    it.setId(rs.getInt("id"));
                    it.setTitle(rs.getString("title"));

                    Timestamp st = rs.getTimestamp("start_time");
                    Timestamp et = rs.getTimestamp("end_time");

                    it.setStartTime(st == null ? "" : st.toLocalDateTime().format(DT_FMT));
                    it.setEndTime(et == null ? "" : et.toLocalDateTime().format(DT_FMT));
                    list.add(it);
                }
            }
        }

        return list;
    }

    /**
     * ✅ (추가) 특정 회의실 월 단위: 날짜별 예약 건수(BOOKED)
     * - 달력(일자별 count)용
     */
    public List<RoomReservationDayCount> countRoomReservationsByMonth(int roomId, YearMonth ym) throws SQLException {
        String sql = ""
                + "SELECT DATE(start_time) AS d, COUNT(*) AS cnt "
                + "FROM reservation "
                + "WHERE room_id = ? "
                + "  AND status = 'BOOKED' "
                + "  AND start_time >= ? "
                + "  AND start_time < ? "
                + "GROUP BY DATE(start_time) "
                + "ORDER BY d ASC";

        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<RoomReservationDayCount> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("d");
                    int cnt = rs.getInt("cnt");

                    RoomReservationDayCount it = new RoomReservationDayCount();
                    it.setDate(d == null ? "" : d.toString());
                    it.setCount(cnt);
                    list.add(it);
                }
            }
        }

        return list;
    }

    /**
     * 예약 취소 (본인 예약만)
     */
    public boolean cancelMyReservation(int userId, int reservationId) throws SQLException {
        String sql = ""
                + "UPDATE reservation "
                + "SET status='CANCELED' "
                + "WHERE id=? AND user_id=? AND status='BOOKED'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservationId);
            ps.setInt(2, userId);

            return ps.executeUpdate() == 1;
        }
    }
}
