package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationService;
import com.company.meeting.user.dto.UserDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * UserReservationCreateServlet
 * - POST /user/reservations/create : 예약 생성(JSON)
 * 파라미터:
 * - roomId
 * - startAt (yyyy-MM-dd HH:mm)
 * - durationMinutes
 * - title (optional)
 */
@WebServlet("/user/reservations/create")
public class UserReservationCreateServlet extends HttpServlet {

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            UserDTO loginUser = (UserDTO) req.getSession().getAttribute("LOGIN_USER");
            if (loginUser == null) {
                JsonUtil.writeJson(resp, ApiResponse.fail("로그인이 필요합니다."));
                return;
            }

            int userId = loginUser.getId();

            int roomId = parseInt(req.getParameter("roomId"), 0);
            String startAt = req.getParameter("startAt");
            int durationMinutes = parseInt(req.getParameter("durationMinutes"), 0);
            String title = req.getParameter("title");

            int newId = reservationService.createReservation(userId, roomId, title, startAt, durationMinutes);

            Map<String, Object> data = new HashMap<>();
            data.put("id", newId);

            JsonUtil.writeJson(resp, ApiResponse.ok(data));

        } catch (Exception e) {
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "예약 생성 실패"));
        }
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v); } catch (Exception ignore) { return def; }
    }
}
