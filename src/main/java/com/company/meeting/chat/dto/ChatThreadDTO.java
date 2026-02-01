package com.company.meeting.chat.dto;

/**
 * ChatThreadDTO
 * - chat_thread 테이블 매핑 DTO
 * - 회원당 1개 thread 정책(UNIQUE user_id)
 */
public class ChatThreadDTO {

    private int id;
    private int userId;
    private String status;

    public ChatThreadDTO() {}

    public ChatThreadDTO(int id, int userId, String status) {
        this.id = id;
        this.userId = userId;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
