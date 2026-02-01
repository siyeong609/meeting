package com.company.meeting.chat.dto;

/**
 * 채팅 메시지 DTO (응답용)
 * - 프론트가 좌/우 렌더링에 필요한 최소 정보 포함
 */
public class ChatMessageDTO {
    private long id;
    private String senderRole;      // USER | ADMIN
    private Integer senderId;       // user.id
    private String senderLoginId;   // user.login_id
    private String senderName;      // user.name
    private String content;
    private String createdAt;       // yyyy-MM-dd HH:mm:ss

    public ChatMessageDTO() {}

    public ChatMessageDTO(long id, String senderRole, Integer senderId,
                          String senderLoginId, String senderName,
                          String content, String createdAt) {
        this.id = id;
        this.senderRole = senderRole;
        this.senderId = senderId;
        this.senderLoginId = senderLoginId;
        this.senderName = senderName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public Integer getSenderId() { return senderId; }
    public void setSenderId(Integer senderId) { this.senderId = senderId; }

    public String getSenderLoginId() { return senderLoginId; }
    public void setSenderLoginId(String senderLoginId) { this.senderLoginId = senderLoginId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
