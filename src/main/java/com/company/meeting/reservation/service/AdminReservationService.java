package com.company.meeting.reservation.service;

import com.company.meeting.common.util.policy.RoomPolicy;
import com.company.meeting.reservation.dao.ReservationDAO;
import com.company.meeting.reservation.dao.ReservationDAO.ReservationRow;
import com.company.meeting.reservation.dto.ReservationRoomPolicy;
import com.company.meeting.reservation.dto.RoomDayOperating;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * AdminReservationService
 * - 관리자 예약 생성/수정/취소
 * - 정책 검증은 사용자와 동일하게 적용
 */
public class AdminReservationService {

    private final ReservationDAO dao = new ReservationDAO();

    /**
     * 관리자 예약 생성(대리 예약)
     */
    public int create(int userId, int roomId, String dateStr, String startTimeStr, int durationMinutes, String title) throws Exception {
        // ✅ 기본 파라미터 방어
        if (userId <= 0) throw new IllegalArgumentException("userId가 올바르지 않습니다.");
        if (roomId <= 0) throw new IllegalArgumentException("roomId가 올바르지 않습니다.");

        LocalDate date = LocalDate.parse(dateStr.trim());     // yyyy-MM-dd
        LocalTime st = LocalTime.parse(startTimeStr.trim());  // HH:mm

        // ✅ room 정책 조회
        ReservationRoomPolicy policy = dao.findRoomPolicy(roomId);
        if (policy == null) throw new IllegalArgumentException("회의실 정책을 찾을 수 없습니다.");

        // ✅ 동일 정책: 활성 회의실만 예약 가능
        if (!policy.isActive()) {
            throw new IllegalArgumentException("비활성 회의실은 예약할 수 없습니다.");
        }

        // ✅ 기간 제한 검증
        validateAvailableRange(policy, date);

        // ✅ 오픈일수 검증(오늘~N일)
        validateBookingOpenDays(policy, date);

        // ✅ 예약시간(min/max/slot) 검증
        validateDurationAndSlot(policy, st, durationMinutes);

        // ✅ 운영시간 검증
        RoomDayOperating op = dao.findOperatingForDate(roomId, date);
        if (op == null || op.isClosed()) {
            throw new IllegalArgumentException("해당 일자는 휴무입니다.");
        }
        validateWithinOperating(op, date, st, durationMinutes);

        // ✅ 버퍼 값 방어(정책값이 이상하면 바로 튕김)
        RoomPolicy.validateBufferMinutes(policy.getBufferMinutes());

        // ✅ 시작/종료 생성
        LocalDateTime start = date.atTime(st);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        // ✅ 충돌 체크 + insert
        return dao.insertReservationWithConflictCheck(
                userId,
                roomId,
                title,
                start,
                end,
                policy.getBufferMinutes()
        );
    }

    /**
     * 관리자 예약 수정
     * - title, time 변경 가능
     * - status=BOOKED만 수정 가능
     */
    public boolean update(int reservationId, int roomId, String dateStr, String startTimeStr, int durationMinutes, String title) throws Exception {
        if (reservationId <= 0) throw new IllegalArgumentException("id가 올바르지 않습니다.");
        if (roomId <= 0) throw new IllegalArgumentException("roomId가 올바르지 않습니다.");

        ReservationRow row = dao.findReservationRowById(reservationId);
        if (row == null) throw new IllegalArgumentException("예약을 찾을 수 없습니다.");

        // ✅ roomId 안전장치(다른 회의실 예약을 수정 못하게)
        if (row.getRoomId() != roomId) {
            throw new IllegalArgumentException("요청 roomId가 예약 정보와 일치하지 않습니다.");
        }

        // ✅ BOOKED만 수정 가능
        if (!"BOOKED".equalsIgnoreCase(String.valueOf(row.getStatus()))) {
            throw new IllegalArgumentException("취소된 예약은 수정할 수 없습니다.");
        }

        LocalDate date = LocalDate.parse(dateStr.trim());
        LocalTime st = LocalTime.parse(startTimeStr.trim());

        ReservationRoomPolicy policy = dao.findRoomPolicy(roomId);
        if (policy == null) throw new IllegalArgumentException("회의실 정책을 찾을 수 없습니다.");

        if (!policy.isActive()) {
            throw new IllegalArgumentException("비활성 회의실은 예약할 수 없습니다.");
        }

        validateAvailableRange(policy, date);
        validateBookingOpenDays(policy, date);
        validateDurationAndSlot(policy, st, durationMinutes);

        RoomDayOperating op = dao.findOperatingForDate(roomId, date);
        if (op == null || op.isClosed()) {
            throw new IllegalArgumentException("해당 일자는 휴무입니다.");
        }
        validateWithinOperating(op, date, st, durationMinutes);

        RoomPolicy.validateBufferMinutes(policy.getBufferMinutes());

        LocalDateTime start = date.atTime(st);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        // ✅ 충돌 체크(자기 자신 제외) + update
        return dao.updateReservationWithConflictCheck(
                reservationId,
                roomId,
                title,
                start,
                end,
                policy.getBufferMinutes()
        );
    }

    /**
     * 관리자 예약 취소
     */
    public boolean cancel(int reservationId) throws Exception {
        if (reservationId <= 0) throw new IllegalArgumentException("id가 올바르지 않습니다.");
        return dao.cancelReservationByAdmin(reservationId);
    }

    // =========================================================
    // ✅ 검증 메서드들(유틸 추가 없이 service 내부에 둠)
    // =========================================================

    private void validateAvailableRange(ReservationRoomPolicy policy, LocalDate date) {
        // availableStartDate / availableEndDate 는 String으로 들어옴(DAO 구현 기준)
        if (policy.getAvailableStartDate() != null && !policy.getAvailableStartDate().isBlank()) {
            LocalDate s = LocalDate.parse(policy.getAvailableStartDate().trim());
            if (date.isBefore(s)) throw new IllegalArgumentException("예약 가능 시작일 이전입니다.");
        }
        if (policy.getAvailableEndDate() != null && !policy.getAvailableEndDate().isBlank()) {
            LocalDate e = LocalDate.parse(policy.getAvailableEndDate().trim());
            if (date.isAfter(e)) throw new IllegalArgumentException("예약 가능 종료일 이후입니다.");
        }
    }

    private void validateBookingOpenDays(ReservationRoomPolicy policy, LocalDate date) {
        int days = policy.getBookingOpenDaysAhead();
        if (days < 1) throw new IllegalArgumentException("예약 가능 기간 정책이 올바르지 않습니다.");

        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("과거 날짜는 예약할 수 없습니다.");
        }
        LocalDate max = today.plusDays(days);
        if (date.isAfter(max)) {
            throw new IllegalArgumentException("예약 가능 기간(" + days + "일)을 초과했습니다.");
        }
    }

    private void validateDurationAndSlot(ReservationRoomPolicy policy, LocalTime start, int durationMinutes) {
        int min = policy.getMinMinutes();
        int max = policy.getMaxMinutes();
        int slot = policy.getSlotMinutes();

        if (durationMinutes < min) throw new IllegalArgumentException("예약시간은 최소 " + min + "분 이상이어야 합니다.");
        if (durationMinutes > max) throw new IllegalArgumentException("예약시간은 최대 " + max + "분 이하이어야 합니다.");

        // ✅ duration은 slot 단위로만 허용
        if (slot > 0 && (durationMinutes % slot != 0)) {
            throw new IllegalArgumentException("예약시간은 " + slot + "분 단위로만 가능합니다.");
        }

        // ✅ 시작시간도 분 단위로 정렬(예: slot=60이면 09:00,10:00..)
        if (slot > 0 && (start.getMinute() % slot != 0)) {
            throw new IllegalArgumentException("시작시간은 " + slot + "분 단위로만 가능합니다.");
        }
    }

    private void validateWithinOperating(RoomDayOperating op, LocalDate date, LocalTime st, int durationMinutes) {
        // op.open/close는 "HH:mm" 형태
        LocalTime open = LocalTime.parse(op.getOpenTime());
        LocalTime close = LocalTime.parse(op.getCloseTime());

        LocalDateTime start = date.atTime(st);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        LocalDateTime openDt = date.atTime(open);
        LocalDateTime closeDt = date.atTime(close);

        if (start.isBefore(openDt)) throw new IllegalArgumentException("운영 시작 시간 이전입니다.");
        if (end.isAfter(closeDt)) throw new IllegalArgumentException("운영 종료 시간을 초과합니다.");
    }
}
