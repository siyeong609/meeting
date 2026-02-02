package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationStatusService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;
import java.util.Map;

/**
 * RoomStatusMonthServlet
 * - POST /user/rooms/status/month
 * - params: roomId, month(yyyy-MM)
 */
@WebServlet("/user/rooms/status/month")
public class RoomStatusMonthServlet extends HttpServlet {

    private final ReservationStatusService service = new ReservationStatusService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int roomId = parseInt(req.getParameter("roomId"), 0);
            String monthStr = req.getParameter("month");

            if (roomId <= 0) {
                JsonUtil.writeJson(resp, ApiResponse.fail("roomId가 올바르지 않습니다."));
                return;
            }
            if (monthStr == null || monthStr.isBlank()) {
                JsonUtil.writeJson(resp, ApiResponse.fail("month가 필요합니다."));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr.trim());
            Map<String, Object> data = service.getMonthStatus(roomId, ym);

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
