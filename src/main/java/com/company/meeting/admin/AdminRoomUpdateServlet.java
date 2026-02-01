package com.company.meeting.admin;

import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.dto.RoomDetail;
import com.company.meeting.room.dto.RoomOperatingHour;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

/**
 * /admin/rooms/update
 * - POST: room 수정(운영시간 7일 포함)
 */
@WebServlet(urlPatterns = "/admin/rooms/update")
public class AdminRoomUpdateServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Map<String, Object> out = new HashMap<>();

        try {
            RoomDetail room = parseRoomFromParams(req, true);

            boolean ok = roomService.update(room);

            out.put("ok", ok);
            out.put("message", ok ? "수정 완료" : "수정 실패");
            out.put("data", null);
            out.put("page", null);

        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            out.put("data", null);
            out.put("page", null);
        }

        JsonUtil.writeJson(resp, out);
    }

    private RoomDetail parseRoomFromParams(HttpServletRequest req, boolean isUpdate) {
        RoomDetail r = new RoomDetail();

        if (isUpdate) {
            r.setId(parseInt(req.getParameter("id"), 0));
            if (r.getId() <= 0) {
                throw new IllegalArgumentException("id가 올바르지 않습니다.");
            }
        }

        r.setName(req.getParameter("name"));
        r.setLocation(trimToNull(req.getParameter("location")));
        r.setCapacity(parseInt(req.getParameter("capacity"), 1));
        r.setActive("1".equals(req.getParameter("isActive")));

        r.setAvailableStartDate(trimToNull(req.getParameter("availableStartDate")));
        r.setAvailableEndDate(trimToNull(req.getParameter("availableEndDate")));

        r.setSlotMinutes(parseInt(req.getParameter("slotMinutes"), 60));
        r.setMinMinutes(parseInt(req.getParameter("minMinutes"), 60));
        r.setMaxMinutes(parseInt(req.getParameter("maxMinutes"), 240));
        r.setBufferMinutes(parseInt(req.getParameter("bufferMinutes"), 0));
        r.setBookingOpenDaysAhead(parseInt(req.getParameter("bookingOpenDaysAhead"), 30));

        List<RoomOperatingHour> hours = new ArrayList<>();
        for (int dow = 1; dow <= 7; dow++) {
            String closedParam = req.getParameter("dow" + dow + "_closed");
            boolean closed = "1".equals(closedParam) || "on".equalsIgnoreCase(closedParam);

            String open = trimToNull(req.getParameter("dow" + dow + "_open"));
            String close = trimToNull(req.getParameter("dow" + dow + "_close"));

            if (closed) {
                hours.add(new RoomOperatingHour(dow, true, null, null));
            } else {
                if (open == null || close == null) {
                    throw new IllegalArgumentException("운영시간 입력이 누락되었습니다. (dow=" + dow + ")");
                }
                hours.add(new RoomOperatingHour(dow, false, open, close));
            }
        }
        r.setOperatingHours(hours);

        return r;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
