package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * AdminReservationsListServlet
 * - POST /admin/reservations/list : 특정 회의실(roomId)의 예약 목록 JSON
 *
 * params:
 * - roomId (필수)
 * - page (기본 1)
 * - size (기본 10)
 * - q (선택: 제목/회의실명 검색)
 */
@WebServlet("/admin/reservations/list")
public class AdminReservationsListServlet extends HttpServlet {

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // ✅ 관리자 로그인 체크 (필터가 있어도 방어적으로)
            Object admin = req.getSession().getAttribute("LOGIN_ADMIN");
            if (admin == null) {
                resp.setStatus(401);
                JsonUtil.writeJson(resp, ApiResponse.fail("관리자 로그인이 필요합니다."));
                return;
            }

            int roomId = parseInt(req.getParameter("roomId"), 0);
            if (roomId <= 0) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("roomId가 올바르지 않습니다."));
                return;
            }

            int page = parseInt(req.getParameter("page"), 1);
            int size = parseInt(req.getParameter("size"), 10);
            String q = req.getParameter("q");

            Map<String, Object> r = reservationService.listRoomReservations(roomId, q, page, size);

            // ✅ user 응답 규약과 동일: ok(data, page)
            JsonUtil.writeJson(resp, ApiResponse.ok(r.get("data"), r.get("page")));

        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage()));

        } catch (Exception e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() == null ? "목록 조회 실패" : e.getMessage()));
        }
    }

    private int parseInt(String v, int def) {
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception ignore) {
            return def;
        }
    }
}
