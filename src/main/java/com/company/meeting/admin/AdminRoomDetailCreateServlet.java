package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.AdminReservationService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminRoomDetailCreateServlet
 * - POST /admin/rooms/detail/create
 * - params: roomId, userId, date(yyyy-MM-dd), startTime(HH:mm), durationMinutes, title(optional)
 */
@WebServlet("/admin/rooms/detail/create")
public class AdminRoomDetailCreateServlet extends HttpServlet {

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

            int roomId = parseInt(req.getParameter("roomId"), 0);
            int userId = parseInt(req.getParameter("userId"), 0);
            String date = req.getParameter("date");
            String startTime = req.getParameter("startTime");
            int durationMinutes = parseInt(req.getParameter("durationMinutes"), 0);
            String title = req.getParameter("title");

            if (roomId <= 0) throw new IllegalArgumentException("roomId가 올바르지 않습니다.");
            if (userId <= 0) throw new IllegalArgumentException("userId가 올바르지 않습니다.");
            if (date == null || date.isBlank()) throw new IllegalArgumentException("date가 필요합니다.");
            if (startTime == null || startTime.isBlank()) throw new IllegalArgumentException("startTime이 필요합니다.");
            if (durationMinutes <= 0) throw new IllegalArgumentException("durationMinutes가 올바르지 않습니다.");

            int newId = service.create(userId, roomId, date, startTime, durationMinutes, title);

            Map<String, Object> data = new HashMap<>();
            data.put("id", newId);

            JsonUtil.writeJson(resp, ApiResponse.ok(data));

        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() == null ? "예약 생성 실패" : e.getMessage()));
        }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(String.valueOf(s).trim()); } catch (Exception ignore) { return def; }
    }
}
