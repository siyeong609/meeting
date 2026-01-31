package com.company.meeting.admin;

import com.company.meeting.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 관리자 대시보드 Servlet
 * <p>
 * URL:
 * - GET /admin/dashboard
 * <p>
 * 역할:
 * - 로그인된 관리자만 접근
 * - dashboard.jsp로 forward
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 전체 회원 수 조회
        int totalUserCount = userService.getTotalUserCount();

        // JSP로 전달
        req.setAttribute("totalUserCount", totalUserCount);

        req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp")
                .forward(req, resp);
    }
}
