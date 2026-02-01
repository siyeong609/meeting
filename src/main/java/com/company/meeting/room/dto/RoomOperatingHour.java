package com.company.meeting.room.dto;

/**
 * RoomOperatingHour
 * - 요일별 운영시간 DTO
 * - dow: 1=월..7=일 (DB 스키마 기준)
 * - closed=true면 open/close는 null로 둔다.
 */
public class RoomOperatingHour {
    private int dow;
    private boolean closed;
    private String openTime;  // "HH:mm"
    private String closeTime; // "HH:mm"

    public RoomOperatingHour() {}

    public RoomOperatingHour(int dow, boolean closed, String openTime, String closeTime) {
        this.dow = dow;
        this.closed = closed;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public int getDow() { return dow; }
    public void setDow(int dow) { this.dow = dow; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}
