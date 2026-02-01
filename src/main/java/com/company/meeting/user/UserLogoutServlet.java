package com.company.meeting.user;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 회원 로그아웃 Servlet
 *
 * URL:
 * - GET /user/logout : 세션 invalidate 후 회원 로그인 페이지로 이동
 */
@WebServlet("/user/logout")
public class UserLogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        resp.sendRedirect(req.getContextPath() + "/user/auth/login");
    }
}
