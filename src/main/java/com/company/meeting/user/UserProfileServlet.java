package com.company.meeting.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 회원 내정보수정 화면
 *
 * URL:
 * - GET /user/profile : 내정보수정 JSP forward
 *
 * 전제:
 * - UserAuthFilter가 /user/* 보호를 하고 있으므로, 로그인 상태만 접근 가능
 */
@WebServlet("/user/profile")
public class UserProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/user/profile.jsp")
                .forward(req, resp);
    }
}
