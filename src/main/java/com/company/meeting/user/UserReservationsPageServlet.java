package com.company.meeting.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * UserReservationsPageServlet
 * - GET /user/reservations  : 페이지 forward
 */
@WebServlet("/user/reservations")
public class UserReservationsPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("pageTitle", "내 예약");
        req.setAttribute("pageCss", "reservation.css"); // user/layout이 pageCss를 받는 구조라면 사용
        req.getRequestDispatcher("/WEB-INF/views/user/reservation/list.jsp").forward(req, resp);
    }
}
