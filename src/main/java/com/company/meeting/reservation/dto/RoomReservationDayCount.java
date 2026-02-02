package com.company.meeting.reservation.dto;

/**
 * RoomReservationDayCount
 * - 달력 렌더링용: 날짜별 예약 건수
 */
public class RoomReservationDayCount {
    private String date; // yyyy-MM-dd
    private int count;

    public RoomReservationDayCount() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}