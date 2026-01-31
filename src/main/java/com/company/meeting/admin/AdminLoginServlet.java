package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.dto.UserDTO;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 관리자 로그인 Servlet
 *
 * URL:
 * - GET  /admin/auth/login : 로그인 페이지 forward
 * - POST /admin/auth/login : 로그인 처리(JSON)
 *
 * 정책:
 * - PBKDF2 해시/평문(마이그레이션) 모두 처리(UserService.authenticate)
 * - role이 ADMIN인 경우만 성공
 * - 성공 시 세션에 LOGIN_ADMIN 저장
 */
@WebServlet("/admin/auth/login")
public class AdminLoginServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/admin/auth/login.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            String userId = req.getParameter("userId");
            String password = req.getParameter("password");

            // 입력 검증
            if (userId == null || userId.isBlank() || password == null || password.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("아이디/비밀번호를 입력하세요.")));
                return;
            }

            // ✅ 해시/평문 모두 검증 + 평문 자동 업그레이드
            UserDTO user = userService.authenticate(userId.trim(), password);

            if (user == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.")));
                return;
            }

            // ADMIN만 허용
            if (user.getRole() == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("관리자 계정이 아닙니다.")));
                return;
            }

            // 세션 저장
            HttpSession session = req.getSession(true);
            session.setAttribute("LOGIN_ADMIN", user);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("ok")));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }
}
