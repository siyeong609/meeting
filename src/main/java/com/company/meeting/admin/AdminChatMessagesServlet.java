package com.company.meeting.admin;

import com.company.meeting.admin.chat.service.AdminChatMessageService;
import com.company.meeting.admin.chat.service.AdminChatMessageService.MessageListResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminChatMessagesServlet
 * - 관리자 채팅 메시지 조회
 *
 * GET /admin/chat/messages?threadId=2&sinceId=0&limit=50
 */
@WebServlet("/admin/chat/messages")
public class AdminChatMessagesServlet extends HttpServlet {

    private static final ObjectMapper OM = new ObjectMapper();
    private final AdminChatMessageService service = new AdminChatMessageService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        int threadId = parseIntOrDefault(request.getParameter("threadId"), 0);
        long sinceId = parseLongOrDefault(request.getParameter("sinceId"), 0);
        int limit = parseIntOrDefault(request.getParameter("limit"), 50);

        try {
            MessageListResult result = service.getMessages(threadId, sinceId, limit);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", result.getItems());
            data.put("nextSinceId", result.getNextSinceId());

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("ok", true);
            root.put("message", "");
            root.put("data", data);

            response.setStatus(200);
            OM.writeValue(response.getWriter(), root);

        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            OM.writeValue(response.getWriter(), errorBody(e.getMessage()));

        } catch (Exception e) {
            response.setStatus(500);
            OM.writeValue(response.getWriter(), errorBody("메시지 조회 중 오류가 발생했습니다."));
        }
    }

    private static int parseIntOrDefault(String s, int def) {
        try {
            if (s == null) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static long parseLongOrDefault(String s, long def) {
        try {
            if (s == null) return def;
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static Map<String, Object> errorBody(String message) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("ok", false);
        root.put("message", message == null ? "오류" : message);
        root.put("data", new LinkedHashMap<>());
        return root;
    }
}
