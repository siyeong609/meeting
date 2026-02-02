package com.company.meeting.reservation.dto;

/**
 * RoomReservationItem
 * - 회의실 특정 일자의 예약 목록 출력용(타임테이블 렌더링)
 */
public class RoomReservationItem {
    private int id;
    private String title;
    private String startTime; // yyyy-MM-dd HH:mm
    private String endTime;   // yyyy-MM-dd HH:mm

    public RoomReservationItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
