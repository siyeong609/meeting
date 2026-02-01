package com.company.meeting.admin.chat.service;

import com.company.meeting.admin.chat.dao.AdminChatMessageDAO;
import com.company.meeting.admin.chat.dto.AdminChatMessageItem;

import java.util.List;

/**
 * AdminChatMessageService
 * - 서블릿에서 오는 입력 검증/보정
 */
public class AdminChatMessageService {

    private final AdminChatMessageDAO dao = new AdminChatMessageDAO();

    public MessageListResult getMessages(int threadId, long sinceId, int limit) {
        if (threadId <= 0) throw new IllegalArgumentException("threadId 값이 올바르지 않습니다.");
        if (sinceId < 0) sinceId = 0;

        if (limit <= 0) limit = 50;
        if (limit > 200) limit = 200;

        // ✅ thread 존재 검증(없으면 404로 처리하는게 낫지만 여기선 예외로 올림)
        if (!dao.existsThread(threadId)) {
            throw new IllegalArgumentException("존재하지 않는 threadId 입니다.");
        }

        List<AdminChatMessageItem> items = dao.selectMessages(threadId, sinceId, limit);

        long nextSinceId = sinceId;
        for (AdminChatMessageItem it : items) {
            if (it.getId() > nextSinceId) nextSinceId = it.getId();
        }

        return new MessageListResult(items, nextSinceId);
    }

    public AdminChatMessageItem sendAdminMessage(int threadId, Integer adminIdOrNull, String content) {
        if (threadId <= 0) throw new IllegalArgumentException("threadId 값이 올바르지 않습니다.");

        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) throw new IllegalArgumentException("content가 비어있습니다.");
        if (c.length() > 2000) throw new IllegalArgumentException("content가 너무 깁니다(최대 2000자).");

        if (!dao.existsThread(threadId)) {
            throw new IllegalArgumentException("존재하지 않는 threadId 입니다.");
        }

        return dao.insertAdminMessage(threadId, adminIdOrNull, c);
    }

    public static class MessageListResult {
        private final List<AdminChatMessageItem> items;
        private final long nextSinceId;

        public MessageListResult(List<AdminChatMessageItem> items, long nextSinceId) {
            this.items = items;
            this.nextSinceId = nextSinceId;
        }

        public List<AdminChatMessageItem> getItems() {
            return items;
        }

        public long getNextSinceId() {
            return nextSinceId;
        }
    }
}
