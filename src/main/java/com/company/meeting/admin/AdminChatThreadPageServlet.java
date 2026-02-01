package com.company.meeting.admin;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * AdminChatThreadPageServlet
 * - 관리자 1:1 채팅창 진입 페이지
 *
 * URL:
 * - GET /admin/chat/thread?threadId=123
 * - GET /admin/chat/thread?threadId=123&popup=1  ✅ 팝업 전용(레이아웃 제거)
 *
 * 역할:
 * - threadId 파라미터 검증
 * - popup 여부에 따라 JSP 분기 forward
 */
@WebServlet("/admin/chat/thread")
public class AdminChatThreadPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String threadIdParam = request.getParameter("threadId");

        // ✅ threadId 필수
        if (threadIdParam == null || threadIdParam.trim().isEmpty()) {
            response.sendError(400, "threadId 파라미터가 필요합니다.");
            return;
        }

        // ✅ 숫자 검증
        int threadId;
        try {
            threadId = Integer.parseInt(threadIdParam.trim());
            if (threadId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            response.sendError(400, "threadId 값이 올바르지 않습니다.");
            return;
        }

        request.setAttribute("threadId", threadId);

        // ✅ popup=1이면 "채팅만" 보이는 JSP로 보냄
        boolean popup = "1".equals(request.getParameter("popup"));

        String jsp = popup
                ? "/WEB-INF/views/admin/chat/thread-popup.jsp"
                : "/WEB-INF/views/admin/chat/thread.jsp"; // (선택) 풀 레이아웃 버전 유지

        RequestDispatcher rd = request.getRequestDispatcher(jsp);
        rd.forward(request, response);
    }
}
