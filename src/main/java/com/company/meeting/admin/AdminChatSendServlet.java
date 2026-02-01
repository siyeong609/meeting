package com.company.meeting.admin;

import com.company.meeting.admin.chat.dto.AdminChatMessageItem;
import com.company.meeting.admin.chat.service.AdminChatMessageService;
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
 * AdminChatSendServlet
 * - 관리자 메시지 전송
 *
 * POST /admin/chat/send
 * body: threadId, content
 */
@WebServlet("/admin/chat/send")
public class AdminChatSendServlet extends HttpServlet {

    private static final ObjectMapper OM = new ObjectMapper();
    private final AdminChatMessageService service = new AdminChatMessageService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        int threadId = parseIntOrDefault(request.getParameter("threadId"), 0);
        String content = request.getParameter("content");

        try {
            // ✅ 여기서 관리자ID를 세션에서 꺼내서 넣는 게 정석
            // 예: Integer adminId = (Integer) request.getSession().getAttribute("LOGIN_ADMIN_ID");
            // 지금은 세션 구조가 확정되지 않았으니 null로 처리(스키마도 null 허용)
            Integer adminId = null;

            AdminChatMessageItem saved = service.sendAdminMessage(threadId, adminId, content);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("item", saved);

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
            OM.writeValue(response.getWriter(), errorBody("메시지 전송 중 오류가 발생했습니다."));
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

    private static Map<String, Object> errorBody(String message) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("ok", false);
        root.put("message", message == null ? "오류" : message);
        root.put("data", new LinkedHashMap<>());
        return root;
    }
}
