package com.company.meeting.chat.service;

import com.company.meeting.chat.dao.ChatDAO;
import com.company.meeting.chat.dto.ChatMessageItem;
import com.company.meeting.chat.dto.ChatThreadDTO;

import java.util.List;

/**
 * ChatService
 * - 서블릿에서 DAO 직접 호출하지 않도록 분리
 */
public class ChatService {

    private final ChatDAO chatDAO = new ChatDAO();

    public ChatThreadDTO ensureThread(int userId) throws Exception {
        return chatDAO.ensureThread(userId);
    }

    public List<ChatMessageItem> listMessages(int threadId, long sinceId, int limit) throws Exception {
        return chatDAO.listMessages(threadId, sinceId, limit);
    }

    public ChatMessageItem sendUserMessage(int threadId, int userId, String content) throws Exception {
        return chatDAO.insertUserMessage(threadId, userId, content);
    }
}
