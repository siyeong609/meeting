package com.company.meeting.reservation.service;

import com.company.meeting.reservation.dao.ReservationDAO;
import com.company.meeting.reservation.dto.ReservationListItem;
import com.company.meeting.reservation.dto.ReservationRoomPolicy;
import com.company.meeting.reservation.dto.RoomDayOperating;
import com.company.meeting.reservation.dto.RoomReservationItem;
import com.company.meeting.reservation.dto.RoomReservationDayCount;

import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReservationService
 * - "즉시 예약" 정책 기준
 * - 검증(운영시간/slot/min/max/기간/충돌) 후 DAO 호출
 */
public class ReservationService {

    private static final DateTimeFormatter DT_IN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReservationDAO reservationDAO = new ReservationDAO();

    /**
     * ✅ 관리자/사용자 공용(회의실 기준): 특정 일자 예약 목록
     * - DAO에 이미 구현된 findRoomReservationsForDate를 그대로 사용
     */
    public List<RoomReservationItem> listRoomReservationsForDate(int roomId, LocalDate date) throws SQLException {
        if (roomId <= 0) throw new IllegalArgumentException("회의실 ID가 올바르지 않습니다.");
        if (date == null) throw new IllegalArgumentException("date가 올바르지 않습니다.");
        return reservationDAO.findRoomReservationsForDate(roomId, date);
    }

    /**
     * ✅ 관리자/사용자 공용(회의실 기준): 월 단위 날짜별 예약 건수
     * - DAO에 이미 구현된 countRoomReservationsByMonth를 그대로 사용
     */
    public List<RoomReservationDayCount> countRoomReservationsByMonth(int roomId, YearMonth ym) throws SQLException {
        if (roomId <= 0) throw new IllegalArgumentException("회의실 ID가 올바르지 않습니다.");
        if (ym == null) throw new IllegalArgumentException("ym이 올바르지 않습니다.");
        return reservationDAO.countRoomReservationsByMonth(roomId, ym);
    }

    // =========================================================
    // ✅ (추가) 관리자/회의실 상세: 회의실 예약 목록(페이징)
    // - ReservationListItem 재사용
    // - q는 제목/회의실명 기준 필터(DAO와 동일)
    // =========================================================
    public Map<String, Object> listRoomReservations(int roomId, String q, int page, int size) throws SQLException {
        if (roomId <= 0) throw new IllegalArgumentException("roomId가 올바르지 않습니다.");

        if (page < 1) page = 1;
        if (size < 1) size = 10;

        int total = reservationDAO.countRoomReservations(roomId, q);
        int totalPages = (int) Math.ceil(total / (double) size);
        int offset = (page - 1) * size;

        List<ReservationListItem> items = reservationDAO.findRoomReservations(roomId, q, offset, size);

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);

        Map<String, Object> pageMeta = new HashMap<>();
        pageMeta.put("page", page);
        pageMeta.put("size", size);
        pageMeta.put("total", total);
        pageMeta.put("totalPages", Math.max(totalPages, 1));

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("page", pageMeta);
        return result;
    }

    public Map<String, Object> listMyReservations(int userId, String q, int page, int size) throws SQLException {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        int total = reservationDAO.countMyReservations(userId, q);
        int totalPages = (int) Math.ceil(total / (double) size);
        int offset = (page - 1) * size;

        List<ReservationListItem> items = reservationDAO.findMyReservations(userId, q, offset, size);

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);

        Map<String, Object> pageMeta = new HashMap<>();
        pageMeta.put("page", page);
        pageMeta.put("size", size);
        pageMeta.put("total", total);
        pageMeta.put("totalPages", Math.max(totalPages, 1));

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("page", pageMeta);
        return result;
    }

    /**
     * 예약 생성
     * @param startAtStr "yyyy-MM-dd HH:mm"
     * @param durationMinutes 사용자가 선택한 예약 시간(분)
     */
    public int createReservation(int userId, int roomId, String title, String startAtStr, int durationMinutes) throws SQLException {
        if (userId <= 0) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (roomId <= 0) throw new IllegalArgumentException("회의실 ID가 올바르지 않습니다.");

        if (startAtStr == null || startAtStr.trim().isEmpty()) {
            throw new IllegalArgumentException("예약 시작 시간을 입력하세요.");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("예약 시간을 선택하세요.");
        }

        ReservationRoomPolicy policy = reservationDAO.findRoomPolicy(roomId);
        if (policy == null) throw new IllegalArgumentException("회의실 정보를 찾을 수 없습니다.");
        if (!policy.isActive()) throw new IllegalArgumentException("비활성화된 회의실입니다.");

        LocalDateTime start = LocalDateTime.parse(startAtStr.trim(), DT_IN);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        // 1) 같은 날짜 내에서만 (단순화)
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new IllegalArgumentException("예약은 하루 안에서만 가능합니다.");
        }

        // 2) 예약 가능 기간(available_start/end)
        LocalDate d = start.toLocalDate();
        if (policy.getAvailableStartDate() != null && !policy.getAvailableStartDate().isBlank()) {
            LocalDate s = LocalDate.parse(policy.getAvailableStartDate());
            if (d.isBefore(s)) throw new IllegalArgumentException("해당 회의실은 " + s + "부터 예약 가능합니다.");
        }
        if (policy.getAvailableEndDate() != null && !policy.getAvailableEndDate().isBlank()) {
            LocalDate e = LocalDate.parse(policy.getAvailableEndDate());
            if (d.isAfter(e)) throw new IllegalArgumentException("해당 회의실은 " + e + "까지만 예약 가능합니다.");
        }

        // 3) booking_open_days_ahead (오늘 기준 N일 앞까지만)
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(policy.getBookingOpenDaysAhead());
        if (d.isAfter(limit)) {
            throw new IllegalArgumentException("해당 회의실은 " + policy.getBookingOpenDaysAhead() + "일 앞까지만 예약 가능합니다.");
        }

        // 4) slot/min/max
        int slot = policy.getSlotMinutes();
        int min = policy.getMinMinutes();
        int max = policy.getMaxMinutes();

        if (durationMinutes < min || durationMinutes > max) {
            throw new IllegalArgumentException("예약 시간은 " + min + "~" + max + "분 범위여야 합니다.");
        }
        if (durationMinutes % slot != 0) {
            throw new IllegalArgumentException("예약 시간은 " + slot + "분 단위로만 가능합니다.");
        }

        // start 시간도 slot 경계로 제한(권장)
        int minuteOfDay = start.getHour() * 60 + start.getMinute();
        if (minuteOfDay % slot != 0) {
            throw new IllegalArgumentException("예약 시작 시간은 " + slot + "분 단위로만 가능합니다.");
        }

        // 5) 운영시간(예외 > 주간)
        RoomDayOperating op = reservationDAO.findOperatingForDate(roomId, d);
        if (op.isClosed()) {
            String msg = "해당 날짜는 휴무입니다.";
            if (op.getReason() != null && !op.getReason().isBlank()) msg += " (" + op.getReason() + ")";
            throw new IllegalArgumentException(msg);
        }

        LocalTime open = LocalTime.parse(op.getOpenTime(), TIME_FMT);
        LocalTime close = LocalTime.parse(op.getCloseTime(), TIME_FMT);

        // 운영 범위 내 포함 (start>=open, end<=close)
        if (start.toLocalTime().isBefore(open) || end.toLocalTime().isAfter(close)) {
            throw new IllegalArgumentException("운영시간(" + op.getOpenTime() + " ~ " + op.getCloseTime() + ") 내에서만 예약 가능합니다.");
        }

        // 6) 충돌 체크 + insert
        int buffer = policy.getBufferMinutes();
        return reservationDAO.insertReservationWithConflictCheck(userId, roomId, title, start, end, buffer);
    }

    public boolean cancelMyReservation(int userId, int reservationId) throws SQLException {
        if (userId <= 0) throw new IllegalArgumentException("로그인이 필요합니다.");
        if (reservationId <= 0) throw new IllegalArgumentException("예약 ID가 올바르지 않습니다.");

        return reservationDAO.cancelMyReservation(userId, reservationId);
    }
}
