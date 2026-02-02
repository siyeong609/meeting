package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationStatusService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * RoomStatusDayServlet
 * - POST /user/rooms/status/day
 * - params: roomId, date(yyyy-MM-dd)
 */
@WebServlet("/user/rooms/status/day")
public class RoomStatusDayServlet extends HttpServlet {

    private final ReservationStatusService service = new ReservationStatusService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int roomId = parseInt(req.getParameter("roomId"), 0);
            String dateStr = req.getParameter("date");

            if (roomId <= 0) {
                JsonUtil.writeJson(resp, ApiResponse.fail("roomId가 올바르지 않습니다."));
                return;
            }
            if (dateStr == null || dateStr.isBlank()) {
                JsonUtil.writeJson(resp, ApiResponse.fail("date가 필요합니다."));
                return;
            }

            LocalDate date = LocalDate.parse(dateStr.trim());
            Map<String, Object> data = service.getDayStatus(roomId, date);

            JsonUtil.writeJson(resp, ApiResponse.ok(data));
        } catch (Exception e) {
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() == null ? "요청 처리 실패" : e.getMessage()));
        }
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(String.valueOf(s).trim());
        } catch (Exception ignore) {
            return def;
        }
    }
}
