package com.company.meeting.room.dao;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.room.dto.RoomDetail;
import com.company.meeting.room.dto.RoomListItem;
import com.company.meeting.room.dto.RoomOperatingHour;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RoomDAO
 * - room + room_operating_hours CRUD
 * - 생성/수정 시 운영시간(7일)을 트랜잭션으로 함께 처리한다.
 */
public class RoomDAO {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public int countRoomsByQuery(String q) throws SQLException {
        String base = "SELECT COUNT(*) FROM room";
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = hasQ
                ? base + " WHERE name LIKE ? OR IFNULL(location,'') LIKE ?"
                : base;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<RoomListItem> findRoomsByQuery(String q, int offset, int size) throws SQLException {
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = ""
                + "SELECT id, name, location, capacity, is_active, slot_minutes, buffer_minutes, updated_at "
                + "FROM room "
                + (hasQ ? "WHERE name LIKE ? OR IFNULL(location,'') LIKE ? " : "")
                + "ORDER BY id DESC "
                + "LIMIT ? OFFSET ?";

        List<RoomListItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            ps.setInt(idx++, size);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomListItem item = new RoomListItem(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("location"),
                            rs.getInt("capacity"),
                            rs.getInt("is_active") == 1,
                            rs.getInt("slot_minutes"),
                            rs.getInt("buffer_minutes"),
                            rs.getString("updated_at")
                    );
                    list.add(item);
                }
            }
        }

        return list;
    }

    public RoomDetail findRoomDetailById(int roomId) throws SQLException {
        String sqlRoom = ""
                + "SELECT id, name, location, capacity, is_active, "
                + "available_start_date, available_end_date, "
                + "slot_minutes, min_minutes, max_minutes, buffer_minutes, booking_open_days_ahead, "
                + "created_at, updated_at "
                + "FROM room WHERE id = ?";

        String sqlHours = ""
                + "SELECT dow, is_closed, open_time, close_time "
                + "FROM room_operating_hours "
                + "WHERE room_id = ? "
                + "ORDER BY dow ASC";

        try (Connection conn = DBConnection.getConnection()) {
            RoomDetail detail = null;

            try (PreparedStatement ps = conn.prepareStatement(sqlRoom)) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;

                    detail = new RoomDetail();
                    detail.setId(rs.getInt("id"));
                    detail.setName(rs.getString("name"));
                    detail.setLocation(rs.getString("location"));
                    detail.setCapacity(rs.getInt("capacity"));
                    detail.setActive(rs.getInt("is_active") == 1);

                    Date s = rs.getDate("available_start_date");
                    Date e = rs.getDate("available_end_date");
                    detail.setAvailableStartDate(s == null ? null : s.toString());
                    detail.setAvailableEndDate(e == null ? null : e.toString());

                    detail.setSlotMinutes(rs.getInt("slot_minutes"));
                    detail.setMinMinutes(rs.getInt("min_minutes"));
                    detail.setMaxMinutes(rs.getInt("max_minutes"));
                    detail.setBufferMinutes(rs.getInt("buffer_minutes"));
                    detail.setBookingOpenDaysAhead(rs.getInt("booking_open_days_ahead"));

                    detail.setCreatedAt(rs.getString("created_at"));
                    detail.setUpdatedAt(rs.getString("updated_at"));
                }
            }

            // 운영시간 7일 조회(없으면 빈 리스트 -> 서비스에서 기본값 채워도 됨)
            List<RoomOperatingHour> hours = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlHours)) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int dow = rs.getInt("dow");
                        boolean closed = rs.getInt("is_closed") == 1;

                        Time ot = rs.getTime("open_time");
                        Time ct = rs.getTime("close_time");

                        String openStr = (ot == null) ? null : ot.toLocalTime().format(TIME_FMT);
                        String closeStr = (ct == null) ? null : ct.toLocalTime().format(TIME_FMT);

                        hours.add(new RoomOperatingHour(dow, closed, openStr, closeStr));
                    }
                }
            }

            detail.setOperatingHours(hours);
            return detail;
        }
    }

    /**
     * 생성(룸 + 운영시간 7일)
     *
     * @return 생성된 room_id
     */
    public int insertRoomWithHours(RoomDetail room) throws SQLException {
        String sqlRoom = ""
                + "INSERT INTO room "
                + "(name, location, capacity, is_active, available_start_date, available_end_date, "
                + " slot_minutes, min_minutes, max_minutes, buffer_minutes, booking_open_days_ahead) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String sqlHours = ""
                + "INSERT INTO room_operating_hours "
                + "(room_id, dow, is_closed, open_time, close_time) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int roomId;

                try (PreparedStatement ps = conn.prepareStatement(sqlRoom, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, room.getName());
                    ps.setString(2, room.getLocation());
                    ps.setInt(3, room.getCapacity());
                    ps.setInt(4, room.isActive() ? 1 : 0);

                    // 날짜는 NULL 허용
                    if (room.getAvailableStartDate() == null || room.getAvailableStartDate().isBlank()) {
                        ps.setNull(5, Types.DATE);
                    } else {
                        ps.setDate(5, Date.valueOf(LocalDate.parse(room.getAvailableStartDate())));
                    }
                    if (room.getAvailableEndDate() == null || room.getAvailableEndDate().isBlank()) {
                        ps.setNull(6, Types.DATE);
                    } else {
                        ps.setDate(6, Date.valueOf(LocalDate.parse(room.getAvailableEndDate())));
                    }

                    ps.setInt(7, room.getSlotMinutes());
                    ps.setInt(8, room.getMinMinutes());
                    ps.setInt(9, room.getMaxMinutes());
                    ps.setInt(10, room.getBufferMinutes());
                    ps.setInt(11, room.getBookingOpenDaysAhead());

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("room 생성 키를 가져오지 못했습니다.");
                        }
                        roomId = keys.getInt(1);
                    }
                }

                // 운영시간 batch insert (7일)
                try (PreparedStatement ps = conn.prepareStatement(sqlHours)) {
                    for (RoomOperatingHour h : room.getOperatingHours()) {
                        ps.setInt(1, roomId);
                        ps.setInt(2, h.getDow());
                        ps.setInt(3, h.isClosed() ? 1 : 0);

                        if (h.isClosed()) {
                            ps.setNull(4, Types.TIME);
                            ps.setNull(5, Types.TIME);
                        } else {
                            ps.setTime(4, Time.valueOf(LocalTime.parse(h.getOpenTime(), TIME_FMT)));
                            ps.setTime(5, Time.valueOf(LocalTime.parse(h.getCloseTime(), TIME_FMT)));
                        }

                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return roomId;

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 수정(룸 + 운영시간 7일)
     */
    public boolean updateRoomWithHours(RoomDetail room) throws SQLException {
        String sqlRoom = ""
                + "UPDATE room SET "
                + "name=?, location=?, capacity=?, is_active=?, "
                + "available_start_date=?, available_end_date=?, "
                + "slot_minutes=?, min_minutes=?, max_minutes=?, buffer_minutes=?, booking_open_days_ahead=? "
                + "WHERE id=?";

        String sqlDeleteHours = "DELETE FROM room_operating_hours WHERE room_id = ?";
        String sqlInsertHours = ""
                + "INSERT INTO room_operating_hours "
                + "(room_id, dow, is_closed, open_time, close_time) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int updated;

                try (PreparedStatement ps = conn.prepareStatement(sqlRoom)) {
                    ps.setString(1, room.getName());
                    ps.setString(2, room.getLocation());
                    ps.setInt(3, room.getCapacity());
                    ps.setInt(4, room.isActive() ? 1 : 0);

                    if (room.getAvailableStartDate() == null || room.getAvailableStartDate().isBlank()) {
                        ps.setNull(5, Types.DATE);
                    } else {
                        ps.setDate(5, Date.valueOf(LocalDate.parse(room.getAvailableStartDate())));
                    }
                    if (room.getAvailableEndDate() == null || room.getAvailableEndDate().isBlank()) {
                        ps.setNull(6, Types.DATE);
                    } else {
                        ps.setDate(6, Date.valueOf(LocalDate.parse(room.getAvailableEndDate())));
                    }

                    ps.setInt(7, room.getSlotMinutes());
                    ps.setInt(8, room.getMinMinutes());
                    ps.setInt(9, room.getMaxMinutes());
                    ps.setInt(10, room.getBufferMinutes());
                    ps.setInt(11, room.getBookingOpenDaysAhead());

                    ps.setInt(12, room.getId());

                    updated = ps.executeUpdate();
                }

                // 기존 운영시간 제거 후 재삽입(단순/안전)
                try (PreparedStatement ps = conn.prepareStatement(sqlDeleteHours)) {
                    ps.setInt(1, room.getId());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlInsertHours)) {
                    for (RoomOperatingHour h : room.getOperatingHours()) {
                        ps.setInt(1, room.getId());
                        ps.setInt(2, h.getDow());
                        ps.setInt(3, h.isClosed() ? 1 : 0);

                        if (h.isClosed()) {
                            ps.setNull(4, Types.TIME);
                            ps.setNull(5, Types.TIME);
                        } else {
                            ps.setTime(4, Time.valueOf(LocalTime.parse(h.getOpenTime(), TIME_FMT)));
                            ps.setTime(5, Time.valueOf(LocalTime.parse(h.getCloseTime(), TIME_FMT)));
                        }

                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return updated == 1;

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * 삭제(단건)
     * - 예약이 존재하면 FK로 실패할 수 있다.
     */
    public boolean deleteRoomById(int roomId) throws SQLException {
        String sql = "DELETE FROM room WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * 회의실 다건 삭제
     * - ids: [1,2,3]
     * - FK(예약 등)로 인해 특정 id가 실패할 수 있으므로, "개별 삭제"로 부분 성공을 허용한다.
     *
     * @return 실제 삭제된 row 수 합계
     */
    public int deleteRoomsByIds(java.util.List<Integer> ids) throws java.sql.SQLException {
        if (ids == null || ids.isEmpty()) return 0;

        String sql = "DELETE FROM room WHERE id = ?";
        int deleted = 0;

        try (java.sql.Connection conn = com.company.meeting.common.db.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Integer id : ids) {
                if (id == null || id <= 0) continue;

                ps.setInt(1, id);
                try {
                    deleted += ps.executeUpdate();
                } catch (java.sql.SQLException ex) {
                    // ✅ 특정 ID만 실패해도 전체 중단하지 않음(부분 성공 허용)
                    // 로그를 남기고 싶으면 여기서 logger 처리
                }
            }
        }

        return deleted;
    }

    // ✅ 사용자용: 활성 회의실만 카운트
    public int countActiveRoomsByQuery(String q) throws SQLException {
        String base = "SELECT COUNT(*) FROM room WHERE is_active = 1";
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = hasQ
                ? base + " AND (name LIKE ? OR IFNULL(location,'') LIKE ?)"
                : base;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ✅ 사용자용: 활성 회의실만 조회
    public List<RoomListItem> findActiveRoomsByQuery(String q, int offset, int size) throws SQLException {
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = ""
                + "SELECT id, name, location, capacity, is_active, slot_minutes, buffer_minutes, updated_at "
                + "FROM room "
                + "WHERE is_active = 1 "
                + (hasQ ? "AND (name LIKE ? OR IFNULL(location,'') LIKE ?) " : "")
                + "ORDER BY id DESC "
                + "LIMIT ? OFFSET ?";

        List<RoomListItem> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            ps.setInt(idx++, size);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomListItem item = new RoomListItem(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("location"),
                            rs.getInt("capacity"),
                            rs.getInt("is_active") == 1,
                            rs.getInt("slot_minutes"),
                            rs.getInt("buffer_minutes"),
                            rs.getString("updated_at")
                    );
                    list.add(item);
                }
            }
        }

        return list;
    }

}
