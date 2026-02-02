package com.company.meeting.reservation.dto;

/**
 * ReservationListItem
 * - "내 예약 목록" 화면에 필요한 최소 필드만 담는 DTO
 */
public class ReservationListItem {
    private int id;

    private int roomId;
    private String roomName;
    private String roomLocation;

    private String title;
    private String status;     // BOOKED / CANCELED
    private String startTime;  // "yyyy-MM-dd HH:mm"
    private String endTime;    // "yyyy-MM-dd HH:mm"
    private String createdAt;  // "yyyy-MM-dd HH:mm"

    public ReservationListItem() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
