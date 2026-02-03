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
 * AdminRoomDetailUpdateServlet
 * - POST /admin/rooms/detail/update
 * - params: id, roomId, date(yyyy-MM-dd), startTime(HH:mm), durationMinutes, title(optional)
 */
@WebServlet("/admin/rooms/detail/update")
public class AdminRoomDetailUpdateServlet extends HttpServlet {

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
            int roomId = parseInt(req.getParameter("roomId"), 0);
            String date = req.getParameter("date");
            String startTime = req.getParameter("startTime");
            int durationMinutes = parseInt(req.getParameter("durationMinutes"), 0);
            String title = req.getParameter("title");

            if (id <= 0) throw new IllegalArgumentException("id가 올바르지 않습니다.");
            if (roomId <= 0) throw new IllegalArgumentException("roomId가 올바르지 않습니다.");
            if (date == null || date.isBlank()) throw new IllegalArgumentException("date가 필요합니다.");
            if (startTime == null || startTime.isBlank()) throw new IllegalArgumentException("startTime이 필요합니다.");
            if (durationMinutes <= 0) throw new IllegalArgumentException("durationMinutes가 올바르지 않습니다.");

            boolean ok = service.update(id, roomId, date, startTime, durationMinutes, title);
            if (!ok) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("수정할 수 없습니다. (이미 취소되었거나 조건 불일치)"));
                return;
            }

            JsonUtil.writeJson(resp, ApiResponse.ok(true));

        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() == null ? "예약 수정 실패" : e.getMessage()));
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(String.valueOf(s).trim()); } catch (Exception ignore) { return def; }
    }
}
