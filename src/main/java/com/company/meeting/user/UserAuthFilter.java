package com.company.meeting.user;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 회원 인증 필터
 *
 * 적용 경로:
 *  - /user/*
 *
 * 예외:
 *  - /user/auth/login (로그인 페이지/로그인 처리)
 *
 * 세션 키:
 *  - LOGIN_USER
 */
@WebFilter("/user/*")
public class UserAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String ctx = req.getContextPath();

        // ✅ 로그인 URL은 필터 제외
        if (uri.equals(ctx + "/user/auth/login")) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ 로그아웃은 로그인 없어도 호출 가능(선택)
        if (uri.equals(ctx + "/user/logout")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        // ✅ 미로그인 → 로그인 페이지로 이동
        if (session == null || session.getAttribute("LOGIN_USER") == null) {
            resp.sendRedirect(ctx + "/user/auth/login");
            return;
        }

        // ✅ 로그인 상태면 통과
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
