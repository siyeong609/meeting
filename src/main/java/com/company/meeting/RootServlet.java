package com.company.meeting;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 애플리케이션 루트 진입 Servlet
 * <p>
 * URL:
 * - /meeting/
 * <p>
 * 역할:
 * - index.jsp로 forward
 * - 실제 JSP는 WEB-INF 아래에 위치
 */
@WebServlet("/home")
public class RootServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // WEB-INF 내부 index.jsp로 forward
        req.getRequestDispatcher("/WEB-INF/views/index.jsp")
                .forward(req, resp);
    }
}
