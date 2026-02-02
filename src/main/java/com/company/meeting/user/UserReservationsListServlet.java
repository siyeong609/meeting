package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationService;
import com.company.meeting.user.dto.UserDTO;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * UserReservationsListServlet
 * - POST /user/reservations/list : 내 예약 목록 JSON
 */
@WebServlet("/user/reservations/list")
public class UserReservationsListServlet extends HttpServlet {

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            UserDTO loginUser = (UserDTO) req.getSession().getAttribute("LOGIN_USER");
            if (loginUser == null) {
                JsonUtil.writeJson(resp, ApiResponse.fail("로그인이 필요합니다."));
                return;
            }

            int userId = loginUser.getId(); // ✅ UserDTO에 getId()가 있다는 전제(서버코드는 컴파일 기반)

            int page = parseInt(req.getParameter("page"), 1);
            int size = parseInt(req.getParameter("size"), 10);
            String q = req.getParameter("q");

            Map<String, Object> r = reservationService.listMyReservations(userId, q, page, size);
            JsonUtil.writeJson(resp, ApiResponse.ok(r.get("data"), r.get("page")));

        } catch (Exception e) {
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "목록 조회 실패"));
        }
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v); } catch (Exception ignore) { return def; }
    }
}
