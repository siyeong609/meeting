package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.reservation.service.ReservationStatusService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * AdminRoomStatusDayServlet
 * - POST /admin/rooms/status/day
 * - params: roomId, date(yyyy-MM-dd)
 *
 * 응답 형태(권장):
 * ApiResponse.ok({
 *   date: "2026-02-02",
 *   open: "09:00",
 *   close: "18:00",
 *   slotMinutes: 60,
 *   reservations: [
 *     { id:1, title:"...", startTime:"yyyy-MM-dd HH:mm", endTime:"yyyy-MM-dd HH:mm", status:"BOOKED", userId:?, userName:? ... }
 *   ]
 * })
 */
@WebServlet("/admin/rooms/status/day")
public class AdminRoomStatusDayServlet extends HttpServlet {

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
            String dateStr = req.getParameter("date");

            if (roomId <= 0) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("roomId가 올바르지 않습니다."));
                return;
            }
            if (dateStr == null || dateStr.isBlank()) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("date가 필요합니다."));
                return;
            }

            // ✅ 핵심: String -> LocalDate
            LocalDate date = LocalDate.parse(dateStr.trim()); // yyyy-MM-dd

            Map<String, Object> data = service.getDayStatus(roomId, date);
            JsonUtil.writeJson(resp, ApiResponse.ok(data));

        } catch (DateTimeParseException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail("date 형식이 올바르지 않습니다. (yyyy-MM-dd)"));

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
