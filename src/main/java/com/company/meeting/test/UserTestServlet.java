package com.company.meeting.test;

import com.company.meeting.user.dto.UserDTO;
import com.company.meeting.user.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * UserService 동작 확인용 테스트 Servlet
 * - DAO 직접 호출 금지
 * - Service 계층 검증 목적
 */
@WebServlet("/test/user")
public class UserTestServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        // 테스트용 로그인 ID
        String loginId = "admin";

        UserDTO user = userService.getUserByLoginId(loginId);

        if (user == null) {
            resp.getWriter().println("USER NOT FOUND");
            return;
        }

        resp.getWriter().println("=== USER SERVICE TEST ===");
        resp.getWriter().println("ID         : " + user.getId());
        resp.getWriter().println("LOGIN_ID   : " + user.getLoginId());
        resp.getWriter().println("NAME       : " + user.getName());
        resp.getWriter().println("ROLE       : " + user.getRole());
        resp.getWriter().println("CREATED_AT : " + user.getCreatedAt());
    }
}
