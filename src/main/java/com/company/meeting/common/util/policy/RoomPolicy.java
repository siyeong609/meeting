package com.company.meeting.common.util.policy;

import java.util.Set;

/**
 * RoomPolicy
 * - 회의실 정책(드롭다운 값 등)을 서버에서 강제 검증하기 위한 유틸
 * - 프론트는 조작 가능하므로, 서버 검증이 최종이다.
 */
public final class RoomPolicy {

    private RoomPolicy() {}

    // ✅ 요구사항: 버퍼 드롭다운 값
    public static final Set<Integer> ALLOWED_BUFFER_MINUTES = Set.of(0, 10, 30, 60);

    /**
     * bufferMinutes 유효성 검증
     */
    public static void validateBufferMinutes(int bufferMinutes) {
        if (!ALLOWED_BUFFER_MINUTES.contains(bufferMinutes)) {
            throw new IllegalArgumentException("버퍼는 0/10/30/60분만 설정 가능합니다.");
        }
    }

    /**
     * 기본 필드 검증(필수값/범위)
     */
    public static void validateBasics(String name, int capacity, int minMinutes, int maxMinutes, int bookingOpenDaysAhead) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("회의실명은 필수입니다.");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("정원은 1 이상이어야 합니다.");
        }
        if (minMinutes < 1) {
            throw new IllegalArgumentException("최소 예약 시간은 1분 이상이어야 합니다.");
        }
        if (maxMinutes < minMinutes) {
            throw new IllegalArgumentException("최대 예약 시간은 최소 예약 시간 이상이어야 합니다.");
        }
        if (bookingOpenDaysAhead < 1) {
            throw new IllegalArgumentException("예약 가능 기간은 1일 이상이어야 합니다.");
        }
    }
}
