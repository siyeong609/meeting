package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.dto.UserDTO;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * 회원 로그인 Servlet
 *
 * URL:
 * - GET  /user/auth/login : 로그인 페이지 forward
 * - POST /user/auth/login : 로그인 처리(JSON)
 *
 * 정책:
 * - PBKDF2 해시/평문(마이그레이션) 모두 처리(UserService.authenticate)
 * - 관리자(ADMIN)는 회원 로그인에서 차단(원하면 정책 바꿔도 됨)
 * - 성공 시 세션에 LOGIN_USER 저장(UserDTO)
 */
@WebServlet("/user/auth/login")
public class UserLoginServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ✅ 이미 로그인 상태면 대시보드로 보냄(불필요한 재로그인 방지)
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("LOGIN_USER") != null) {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/user/auth/login.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            // ✅ 관리자 로그인과 동일 파라미터명으로 받는다.
            String userId = req.getParameter("userId");
            String password = req.getParameter("password");

            // ✅ 입력 검증
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

            // ✅ 정책: 회원 로그인에서는 ADMIN 차단
            // - 만약 ADMIN도 회원 대시보드 접근을 허용하고 싶으면 이 블록을 제거하면 됨
            if (user.getRole() != null && user.getRole().equalsIgnoreCase("ADMIN")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("관리자 계정은 관리자 로그인에서 이용하세요.")));
                return;
            }

            // ✅ 세션 저장: 예약 구현에서 userId를 계속 쓰게 되므로 UserDTO 저장이 편함
            HttpSession session = req.getSession(true);
            session.setAttribute("LOGIN_USER", user);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("ok")));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }

    public static class UserProfileUploadServlet {
    }
}
