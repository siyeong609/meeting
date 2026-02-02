package com.company.meeting.reservation.service;

import com.company.meeting.reservation.dao.ReservationDAO;
import com.company.meeting.reservation.dto.ReservationRoomPolicy;
import com.company.meeting.reservation.dto.RoomDayOperating;
import com.company.meeting.reservation.dto.RoomReservationDayCount;
import com.company.meeting.reservation.dto.RoomReservationItem;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReservationStatusService
 * - user/room/list 하단 "예약 현황" 카드에서 사용할 데이터 제공
 * - 일자(타임테이블): 운영시간 + slotMinutes + 예약목록
 * - 달력(월): 날짜별 예약 count
 */
public class ReservationStatusService {

    private final ReservationDAO reservationDAO = new ReservationDAO();

    /**
     * 특정 일자 현황
     * @return Map(JSON 응답용)
     */
    public Map<String, Object> getDayStatus(int roomId, LocalDate date) throws SQLException {
        ReservationRoomPolicy policy = reservationDAO.findRoomPolicy(roomId);
        if (policy == null) {
            throw new IllegalArgumentException("회의실 정책을 찾을 수 없습니다. (roomId=" + roomId + ")");
        }
        if (!policy.isActive()) {
            throw new IllegalArgumentException("비활성화된 회의실입니다.");
        }

        RoomDayOperating op = reservationDAO.findOperatingForDate(roomId, date);
        List<RoomReservationItem> reservations = reservationDAO.findRoomReservationsForDate(roomId, date);

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("date", date.toString());

        data.put("slotMinutes", policy.getSlotMinutes());
        data.put("bufferMinutes", policy.getBufferMinutes());

        // 운영 정보
        data.put("closed", op.isClosed());
        data.put("open", op.getOpenTime());
        data.put("close", op.getCloseTime());
        data.put("reason", op.getReason());

        data.put("reservations", reservations);
        return data;
    }

    /**
     * 월 현황(달력)
     */
    public Map<String, Object> getMonthStatus(int roomId, YearMonth ym) throws SQLException {
        ReservationRoomPolicy policy = reservationDAO.findRoomPolicy(roomId);
        if (policy == null) {
            throw new IllegalArgumentException("회의실 정책을 찾을 수 없습니다. (roomId=" + roomId + ")");
        }
        if (!policy.isActive()) {
            throw new IllegalArgumentException("비활성화된 회의실입니다.");
        }

        List<RoomReservationDayCount> days = reservationDAO.countRoomReservationsByMonth(roomId, ym);

        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("month", ym.toString()); // yyyy-MM
        data.put("days", days);
        return data;
    }
}
