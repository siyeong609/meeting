package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.chat.dto.ChatMessageItem;
import com.company.meeting.chat.dto.ChatThreadDTO;
import com.company.meeting.chat.service.ChatService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * UserChatSendServlet
 * - POST /user/chat/send
 *
 * Request:
 * - content: 메시지 본문
 *
 * Response(ApiResponse):
 * - data: 저장된 메시지 1건(ChatMessageItem)
 */
@WebServlet("/user/chat/send")
public class UserChatSendServlet extends HttpServlet {

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

            int userId = extractUserId(loginUserObj);
            if (userId <= 0) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("로그인 정보가 올바르지 않습니다.")));
                return;
            }

            String content = req.getParameter("content");
            content = (content == null) ? "" : content.trim();

            if (content.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("메시지를 입력하세요.")));
                return;
            }

            if (content.length() > 1000) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("메시지는 1000자 이하여야 합니다.")));
                return;
            }

            // thread 보장
            ChatThreadDTO thread = chatService.ensureThread(userId);

            // 저장 + 저장된 메시지 반환
            ChatMessageItem saved = chatService.sendUserMessage(thread.getId(), userId, content);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(saved)));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("메시지 전송 중 오류가 발생했습니다.")));
        }
    }

    /**
     * ✅ 로그인 세션 객체에서 id 추출
     * - 우선순위: getId() -> getUserId() -> field(id)
     */
    private int extractUserId(Object loginUserObj) {
        try {
            try {
                Object v = loginUserObj.getClass().getMethod("getId").invoke(loginUserObj);
                if (v != null) return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignore) {}

            try {
                Object v = loginUserObj.getClass().getMethod("getUserId").invoke(loginUserObj);
                if (v != null) return Integer.parseInt(String.valueOf(v));
            } catch (Exception ignore) {}

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
