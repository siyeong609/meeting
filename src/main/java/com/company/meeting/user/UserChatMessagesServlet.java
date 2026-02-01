package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.chat.dto.ChatMessageItem;
import com.company.meeting.chat.dto.ChatThreadDTO;
import com.company.meeting.chat.service.ChatService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserChatMessagesServlet
 * - POST /user/chat/messages
 *
 * Request:
 * - sinceId: 마지막 메시지 ID(없으면 0)
 *
 * Response(ApiResponse):
 * - data: { threadId, items: [...] }
 */
@WebServlet("/user/chat/messages")
public class UserChatMessagesServlet extends HttpServlet {

    private final ChatService chatService = new ChatService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            HttpSession session = req.getSession(false);
            Object loginUserObj = (session == null) ? null : session.getAttribute("LOGIN_USER");

            if (loginUserObj == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("로그인이 필요합니다.")));
                return;
            }

            // ✅ UserDTO 메서드명 확정 전에도 컴파일 되도록 리플렉션으로 id 추출
            int userId = extractUserId(loginUserObj);
            if (userId <= 0) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("로그인 정보가 올바르지 않습니다.")));
                return;
            }

            long sinceId = 0;
            try {
                String s = req.getParameter("sinceId");
                if (s != null && !s.isBlank()) sinceId = Long.parseLong(s.trim());
            } catch (Exception ignore) {
                sinceId = 0;
            }

            // thread 보장
            ChatThreadDTO thread = chatService.ensureThread(userId);

            // 메시지 조회(최초 50개 / 이후 신규)
            List<ChatMessageItem> items = chatService.listMessages(thread.getId(), sinceId, 50);

            Map<String, Object> data = new HashMap<>();
            data.put("threadId", thread.getId());
            data.put("items", items);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(data)));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("채팅 메시지 조회 중 오류가 발생했습니다.")));
        }
    }

    /**
     * ✅ 로그인 세션 객체에서 id 추출
     * - 우선순위: getId() -> getUserId() -> field(id) 시도
     */
    private int extractUserId(Object loginUserObj) {
        try {
            // getId()
            try {
                Object v = loginUserObj.getClass().getMethod("getId").invoke(loginUserObj);
                if (v != null) return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignore) {}

            // getUserId()
            try {
                Object v = loginUserObj.getClass().getMethod("getUserId").invoke(loginUserObj);
                if (v != null) return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignore) {}

            // field id
            try {
                java.lang.reflect.Field f = loginUserObj.getClass().getDeclaredField("id");
                f.setAccessible(true);
                Object v = f.get(loginUserObj);
                if (v != null) return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignore) {}

        } catch (Exception ignore) {}

        return 0;
    }
}
