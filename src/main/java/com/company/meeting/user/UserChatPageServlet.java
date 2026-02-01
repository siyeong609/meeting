package com.company.meeting.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 회원 채팅 페이지 (fallback 용도)
 *
 * URL:
 * - GET /user/chat
 *
 * 목적:
 * - 헤더 메뉴 href(/user/chat)가 JS에 의해 위젯으로 전환되지 못했을 때도
 *   404가 아니라 정상 페이지가 뜨도록 보장한다.
 *
 * 동작:
 * - chat.jsp로 forward
 */
@WebServlet("/user/chat")
public class UserChatPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/user/chat.jsp")
                .forward(req, resp);
    }
}
