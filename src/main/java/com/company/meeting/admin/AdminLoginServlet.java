package com.company.meeting.admin;

import com.company.meeting.user.dto.UserDTO;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 관리자 로그인 Servlet
 * <p>
 * URL:
 * - GET  /admin/auth/login  : 로그인 페이지
 * - POST /admin/auth/login  : 로그인 처리 (AJAX)
 */
@WebServlet("/admin/auth/login")
public class AdminLoginServlet extends HttpServlet {

    private final UserService userService = new UserService();

    /**
     * 로그인 페이지 출력
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 로그인 JSP로 forward
        req.getRequestDispatcher("/WEB-INF/views/admin/auth/login.jsp")
                .forward(req, resp);
    }

    /**
     * 로그인 처리 (AJAX)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        System.out.println("### ADMIN LOGIN POST CALLED ###");
        resp.setContentType("application/json; charset=UTF-8");

        String userId = req.getParameter("userId");
        String password = req.getParameter("password");

        UserDTO user = userService.getUserByLoginId(userId);

        if (user == null ||
                !user.getPassword().equals(password) ||
                !"ADMIN".equals(user.getRole())) {

            resp.getWriter().write("""
                        {
                          "success": false,
                          "message": "아이디 또는 비밀번호가 올바르지 않습니다."
                        }
                    """);
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("LOGIN_ADMIN", user);

        resp.getWriter().write("""
                    {
                      "success": true
                    }
                """);
    }
}
