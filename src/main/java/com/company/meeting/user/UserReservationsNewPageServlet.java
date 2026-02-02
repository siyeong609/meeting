package com.company.meeting.user;

import com.company.meeting.user.dto.UserDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * UserReservationsNewPageServlet
 * - GET /user/reservations/new : 예약 생성 페이지(new.jsp)로 forward
 * - WEB-INF 아래 JSP는 직접 접근이 불가하므로 "페이지 서블릿"이 필요하다.
 */
@WebServlet("/user/reservations/new")
public class UserReservationsNewPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ✅ 로그인 체크 (원하면 필터로 빼도 됨)
        UserDTO loginUser = (UserDTO) req.getSession().getAttribute("LOGIN_USER");
        if (loginUser == null) {
            resp.sendRedirect(req.getContextPath() + "/user/login");
            return;
        }

        req.setAttribute("pageTitle", "예약 생성");
        req.setAttribute("pageCss", "reservation.css");

        req.getRequestDispatcher("/WEB-INF/views/user/reservation/new.jsp").forward(req, resp);
    }
}
