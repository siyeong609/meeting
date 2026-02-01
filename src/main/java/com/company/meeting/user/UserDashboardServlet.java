package com.company.meeting.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 회원 대시보드 Servlet
 *
 * URL:
 * - GET /user/dashboard : 대시보드 JSP forward
 *
 * 주의:
 * - /user/* 보호는 UserAuthFilter가 담당
 */
@WebServlet("/user/dashboard")
public class UserDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/user/dashboard.jsp")
                .forward(req, resp);
    }
}
