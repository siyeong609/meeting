package com.company.meeting.test;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 첫 Servlet 테스트용
 * URL 매핑 및 Tomcat 연동 확인 목적
 */
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // 응답 타입 설정
        resp.setContentType("text/plain; charset=UTF-8");

        // 단순 문자열 출력
        resp.getWriter().write("HELLO SERVLET OK");
    }
}
