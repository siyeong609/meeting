package com.company.meeting.room.service;

import com.company.meeting.common.util.policy.RoomPolicy;
import com.company.meeting.room.dao.RoomDAO;
import com.company.meeting.room.dto.RoomDetail;
import com.company.meeting.room.dto.RoomListItem;
import com.company.meeting.room.dto.RoomOperatingHour;

import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * RoomService
 * - DAO 호출 전 입력값 검증/기본값 보정 담당
 */
public class RoomService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final RoomDAO roomDAO = new RoomDAO();

    public int countRooms(String q) throws SQLException {
        return roomDAO.countRoomsByQuery(q);
    }

    public List<RoomListItem> listRooms(String q, int page, int size) throws SQLException {
        int offset = (page - 1) * size;
        return roomDAO.findRoomsByQuery(q, offset, size);
    }

    public RoomDetail getRoomDetail(int id) throws SQLException {
        RoomDetail d = roomDAO.findRoomDetailById(id);
        if (d == null) return null;

        // 운영시간이 7개가 아니면(데이터 누락 등) 기본값으로 채운다.
        fillDefaultHoursIfMissing(d);
        return d;
    }

    public int create(RoomDetail room) throws SQLException {
        validate(room);
        fillDefaultHoursIfMissing(room);
        return roomDAO.insertRoomWithHours(room);
    }

    public boolean update(RoomDetail room) throws SQLException {
        if (room.getId() <= 0) {
            throw new IllegalArgumentException("room id가 올바르지 않습니다.");
        }
        validate(room);
        fillDefaultHoursIfMissing(room);
        return roomDAO.updateRoomWithHours(room);
    }

    public boolean delete(int roomId) throws SQLException {
        if (roomId <= 0) {
            throw new IllegalArgumentException("room id가 올바르지 않습니다.");
        }
        return roomDAO.deleteRoomById(roomId);
    }

    private void validate(RoomDetail room) {
        RoomPolicy.validateBasics(
                room.getName(),
                room.getCapacity(),
                room.getMinMinutes(),
                room.getMaxMinutes(),
                room.getBookingOpenDaysAhead()
        );
        RoomPolicy.validateBufferMinutes(room.getBufferMinutes());

        // 운영시간 검증(휴무면 시간 없어야, 운영이면 open<close)
        List<RoomOperatingHour> hours = room.getOperatingHours();
        if (hours == null || hours.isEmpty()) {
            // 서비스에서 채울거라 여기서 막지는 않음
            return;
        }

        for (RoomOperatingHour h : hours) {
            if (h.getDow() < 1 || h.getDow() > 7) {
                throw new IllegalArgumentException("요일(dow)은 1~7 범위여야 합니다.");
            }

            if (h.isClosed()) {
                continue;
            }

            if (h.getOpenTime() == null || h.getOpenTime().isBlank()
                    || h.getCloseTime() == null || h.getCloseTime().isBlank()) {
                throw new IllegalArgumentException("운영 요일의 오픈/마감 시간이 비어있습니다. (dow=" + h.getDow() + ")");
            }

            LocalTime open = LocalTime.parse(h.getOpenTime(), TIME_FMT);
            LocalTime close = LocalTime.parse(h.getCloseTime(), TIME_FMT);
            if (!open.isBefore(close)) {
                throw new IllegalArgumentException("운영시간은 open < close 여야 합니다. (dow=" + h.getDow() + ")");
            }
        }
    }

    /**
     * 운영시간이 비어있거나 7일이 아닌 경우 기본값을 채운다.
     * - 기본: 월~금 09:00~18:00, 토/일 휴무
     */
    private void fillDefaultHoursIfMissing(RoomDetail d) {
        List<RoomOperatingHour> hours = d.getOperatingHours();
        if (hours == null) {
            hours = new ArrayList<>();
            d.setOperatingHours(hours);
        }

        Map<Integer, RoomOperatingHour> map = new HashMap<>();
        for (RoomOperatingHour h : hours) map.put(h.getDow(), h);

        for (int dow = 1; dow <= 7; dow++) {
            if (!map.containsKey(dow)) {
                boolean weekend = (dow == 6 || dow == 7);
                if (weekend) {
                    hours.add(new RoomOperatingHour(dow, true, null, null));
                } else {
                    hours.add(new RoomOperatingHour(dow, false, "09:00", "18:00"));
                }
            }
        }

        // dow 순서 정렬
        hours.sort(Comparator.comparingInt(RoomOperatingHour::getDow));
    }

    /**
     * 회의실 다건 삭제
     * - ids는 1 이상만 유효
     * @return 실제 삭제된 개수
     */
    public int deleteMany(java.util.List<Integer> ids) throws java.sql.SQLException {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 회의실을 선택하세요.");
        }

        java.util.List<Integer> cleaned = new java.util.ArrayList<>();
        for (Integer id : ids) {
            if (id != null && id > 0) cleaned.add(id);
        }

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("삭제할 회의실 ID가 올바르지 않습니다.");
        }

        return roomDAO.deleteRoomsByIds(cleaned);
    }

    /**
     * ✅ 사용자용: 활성 회의실만 카운트
     */
    public int countActiveRooms(String q) throws SQLException {
        return roomDAO.countActiveRoomsByQuery(q);
    }

    /**
     * ✅ 사용자용: 활성 회의실만 목록 조회
     */
    public List<RoomListItem> listActiveRooms(String q, int page, int size) throws SQLException {
        int offset = (page - 1) * size;
        return roomDAO.findActiveRoomsByQuery(q, offset, size);
    }
}
