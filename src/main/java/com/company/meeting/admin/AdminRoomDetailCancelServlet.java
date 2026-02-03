package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.AdminReservationService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * AdminRoomDetailCancelServlet
 * - POST /admin/rooms/detail/cancel
 * - params: id
 */
@WebServlet("/admin/rooms/detail/cancel")
public class AdminRoomDetailCancelServlet extends HttpServlet {

    private final AdminReservationService service = new AdminReservationService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Object loginAdmin = req.getSession().getAttribute("LOGIN_ADMIN");
            if (loginAdmin == null) {
                resp.setStatus(401);
                JsonUtil.writeJson(resp, ApiResponse.fail("관리자 로그인이 필요합니다."));
                return;
            }

            int id = parseInt(req.getParameter("id"), 0);
            if (id <= 0) throw new IllegalArgumentException("id가 올바르지 않습니다.");

            boolean ok = service.cancel(id);
            if (!ok) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("취소할 수 없습니다. (이미 취소되었거나 존재하지 않음)"));
                return;
            }

            JsonUtil.writeJson(resp, ApiResponse.ok(true));

        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() == null ? "예약 취소 실패" : e.getMessage()));
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(String.valueOf(s).trim()); } catch (Exception ignore) { return def; }
    }
}
