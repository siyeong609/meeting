package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationStatusService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * AdminRoomStatusMonthServlet
 * - POST /admin/rooms/status/month
 * - params: roomId, month(yyyy-MM)
 *
 * ✅ 관리자/사용자 동일한 응답 형태 유지:
 * ApiResponse.ok({
 *   month: "2026-02",
 *   days: [ { date:"2026-02-01", count:2 }, ... ]
 * })
 */
@WebServlet("/admin/rooms/status/month")
public class AdminRoomStatusMonthServlet extends HttpServlet {

    private final ReservationStatusService service = new ReservationStatusService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // ✅ 관리자 로그인 체크
            Object loginAdmin = req.getSession().getAttribute("LOGIN_ADMIN");
            if (loginAdmin == null) {
                resp.setStatus(401);
                JsonUtil.writeJson(resp, ApiResponse.fail("관리자 로그인이 필요합니다."));
                return;
            }

            int roomId = parseInt(req.getParameter("roomId"), 0);
            String monthStr = req.getParameter("month");

            if (roomId <= 0) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("roomId가 올바르지 않습니다."));
                return;
            }
            if (monthStr == null || monthStr.isBlank()) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("month가 필요합니다."));
                return;
            }

            YearMonth ym = YearMonth.parse(monthStr.trim()); // yyyy-MM
            Map<String, Object> data = service.getMonthStatus(roomId, ym);

            JsonUtil.writeJson(resp, ApiResponse.ok(data));

        } catch (DateTimeParseException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail("month 형식이 올바르지 않습니다. (yyyy-MM)"));

        } catch (Exception e) {
            resp.setStatus(500);
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
