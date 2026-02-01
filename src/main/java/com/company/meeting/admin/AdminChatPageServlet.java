package com.company.meeting.admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * AdminChatPageServlet
 * - 관리자 대화관리(채팅문의) 목록 페이지 진입용
 * - /admin/chat 으로 들어오면 JSP로 forward
 *
 * 주의:
 * - /admin/* 보호는 AdminAuthFilter가 처리한다고 가정
 */
@WebServlet("/admin/chat")
public class AdminChatPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ✅ JSP 직접 접근 차단 구조 유지: /WEB-INF 아래로 forward
        RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/admin/chat/list.jsp");
        rd.forward(request, response);
    }
}
