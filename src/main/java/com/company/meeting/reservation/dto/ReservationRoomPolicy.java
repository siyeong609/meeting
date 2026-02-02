package com.company.meeting.reservation.dto;

/**
 * ReservationRoomPolicy
 * - 예약 검증에 필요한 room 정책 값만 담는다.
 */
public class ReservationRoomPolicy {
    private boolean active;

    private String availableStartDate; // YYYY-MM-DD or null
    private String availableEndDate;   // YYYY-MM-DD or null

    private int slotMinutes;
    private int minMinutes;
    private int maxMinutes;

    private int bufferMinutes;
    private int bookingOpenDaysAhead;

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
}
