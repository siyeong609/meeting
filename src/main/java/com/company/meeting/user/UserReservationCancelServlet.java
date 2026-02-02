package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationService;
import com.company.meeting.user.dto.UserDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UserReservationCancelServlet
 * - POST /user/reservations/cancel : 예약 취소(JSON)
 * 파라미터:
 * - id (reservation id)
 */
@WebServlet("/user/reservations/cancel")
public class UserReservationCancelServlet extends HttpServlet {

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
            int id = parseInt(req.getParameter("id"), 0);

            boolean ok = reservationService.cancelMyReservation(userId, id);
            if (!ok) {
                JsonUtil.writeJson(resp, ApiResponse.fail("취소할 수 없습니다. (이미 취소되었거나 권한 없음)"));
                return;
            }

            JsonUtil.writeJson(resp, ApiResponse.ok(true));

        } catch (Exception e) {
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "예약 취소 실패"));
        }
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v); } catch (Exception ignore) { return def; }
    }
}
