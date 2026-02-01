package com.company.meeting.chat.dto;

/**
 * ChatMessageItem
 * - 위젯 렌더링용 응답 DTO
 * - JS에서 좌/우 판단(senderRole), meta 표시(senderName/senderLoginId), 시간(createdAt) 사용
 */
public class ChatMessageItem {

    private long id;
    private String senderRole;     // USER | ADMIN
    private String senderName;     // 표시용
    private String senderLoginId;  // 표시용
    private String content;
    private String createdAt;      // yyyy-MM-dd HH:mm:ss

    public ChatMessageItem() {}

    public ChatMessageItem(long id, String senderRole, String senderName, String senderLoginId, String content, String createdAt) {
        this.id = id;
        this.senderRole = senderRole;
        this.senderName = senderName;
        this.senderLoginId = senderLoginId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderLoginId() {
        return senderLoginId;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSenderLoginId(String senderLoginId) {
        this.senderLoginId = senderLoginId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
