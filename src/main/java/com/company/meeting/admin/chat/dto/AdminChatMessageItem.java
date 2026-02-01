package com.company.meeting.admin.chat.dto;

/**
 * AdminChatMessageItem
 * - 관리자/회원 공통으로 쓰기 좋은 메시지 DTO (MVP)
 */
public class AdminChatMessageItem {

    private final long id;
    private final int threadId;
    private final String senderRole; // "USER" | "ADMIN"
    private final Integer senderId;  // USER면 user_id, ADMIN이면 admin_id (없으면 null)
    private final String content;
    private final String createdAt;  // "yyyy-MM-dd HH:mm:ss"

    public AdminChatMessageItem(long id,
                                int threadId,
                                String senderRole,
                                Integer senderId,
                                String content,
                                String createdAt) {
        this.id = id;
        this.threadId = threadId;
        this.senderRole = senderRole;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public int getThreadId() {
        return threadId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
