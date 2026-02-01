package com.company.meeting.room.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * RoomDetail
 * - 관리자 생성/수정/상세 조회용 DTO
 * - room 테이블 + room_operating_hours(7일) 정보를 함께 담는다.
 */
public class RoomDetail {
    private int id;

    private String name;
    private String location;
    private int capacity;
    private boolean active;

    private String availableStartDate; // "YYYY-MM-DD" or null/empty
    private String availableEndDate;   // "YYYY-MM-DD" or null/empty

    private int slotMinutes;
    private int minMinutes;
    private int maxMinutes;
    private int bufferMinutes;
    private int bookingOpenDaysAhead;

    private String createdAt;
    private String updatedAt;

    private List<RoomOperatingHour> operatingHours = new ArrayList<>();

    public RoomDetail() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getAvailableStartDate() { return availableStartDate; }
    public void setAvailableStartDate(String availableStartDate) { this.availableStartDate = availableStartDate; }

    public String getAvailableEndDate() { return availableEndDate; }
    public void setAvailableEndDate(String availableEndDate) { this.availableEndDate = availableEndDate; }

    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }

    public int getMinMinutes() { return minMinutes; }
    public void setMinMinutes(int minMinutes) { this.minMinutes = minMinutes; }

    public int getMaxMinutes() { return maxMinutes; }
    public void setMaxMinutes(int maxMinutes) { this.maxMinutes = maxMinutes; }

    public int getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }

    public int getBookingOpenDaysAhead() { return bookingOpenDaysAhead; }
    public void setBookingOpenDaysAhead(int bookingOpenDaysAhead) { this.bookingOpenDaysAhead = bookingOpenDaysAhead; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public List<RoomOperatingHour> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(List<RoomOperatingHour> operatingHours) { this.operatingHours = operatingHours; }
}
