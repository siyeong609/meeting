package com.company.meeting.admin.chat.dto;

/**
 * AdminChatThreadListItem
 * - 관리자 스레드 목록 UI에 뿌릴 최소 필드
 *
 * list.js가 기대하는 키:
 * - threadId
 * - userLoginId
 * - status
 * - lastMessagePreview    ✅ 추가 (최근 메시지 내용 1줄 프리뷰)
 * - lastMessageAt         ✅ 유지 (최근 메시지 시간)
 * - createdAt
 */
public class AdminChatThreadListItem {

    private final int threadId;
    private final int userId;
    private final String userLoginId;
    private final String status;

    private final String lastMessagePreview; // ✅ 추가
    private final String lastMessageAt;      // ✅ 최근 메시지 시간
    private final String createdAt;

    public AdminChatThreadListItem(int threadId,
                                   int userId,
                                   String userLoginId,
                                   String status,
                                   String lastMessagePreview,
                                   String lastMessageAt,
                                   String createdAt) {
        this.threadId = threadId;
        this.userId = userId;
        this.userLoginId = userLoginId;
        this.status = status;
        this.lastMessagePreview = lastMessagePreview;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserLoginId() {
        return userLoginId;
    }

    public String getStatus() {
        return status;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public String getLastMessageAt() {
        return lastMessageAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
