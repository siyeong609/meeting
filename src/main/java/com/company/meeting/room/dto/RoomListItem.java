package com.company.meeting.room.dto;

/**
 * RoomListItem
 * - 관리자 목록 화면용 DTO
 * - 목록에 필요한 최소 정보만 포함한다.
 */
public class RoomListItem {
    private int id;
    private String name;
    private String location;
    private int capacity;
    private boolean active;

    private int slotMinutes;
    private int bufferMinutes;

    private String updatedAt; // 화면 표시용(YYYY-MM-DD HH:mm:ss)

    public RoomListItem() {}

    public RoomListItem(int id, String name, String location, int capacity, boolean active,
                        int slotMinutes, int bufferMinutes, String updatedAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.active = active;
        this.slotMinutes = slotMinutes;
        this.bufferMinutes = bufferMinutes;
        this.updatedAt = updatedAt;
    }

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

    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }

    public int getBufferMinutes() { return bufferMinutes; }
    public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
