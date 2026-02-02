package com.company.meeting.reservation.dto;

/**
 * RoomDayOperating
 * - 특정 날짜의 운영시간 결과(예외 > 주간운영)
 */
public class RoomDayOperating {
    private boolean closed;
    private String openTime;   // "HH:mm" or null
    private String closeTime;  // "HH:mm" or null
    private String reason;     // exception reason (optional)

    public RoomDayOperating() {}

    public RoomDayOperating(boolean closed, String openTime, String closeTime, String reason) {
        this.closed = closed;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.reason = reason;
    }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
