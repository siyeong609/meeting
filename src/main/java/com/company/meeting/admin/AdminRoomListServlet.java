package com.company.meeting.admin;

import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.dto.RoomListItem;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /admin/rooms
 * - GET: 관리자 회의실 목록 JSP
 * - POST: 목록 JSON(page/size/q)
 */
@WebServlet(urlPatterns = "/admin/rooms")
public class AdminRoomListServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/admin/room/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String q = trimToNull(req.getParameter("q"));
        int page = parseInt(req.getParameter("page"), 1);
        int size = parseInt(req.getParameter("size"), 10);
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Map<String, Object> out = new HashMap<>();
        try {
            int total = roomService.countRooms(q);
            List<RoomListItem> items = roomService.listRooms(q, page, size);

            int totalPages = (int) Math.ceil(total / (double) size);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("page", page);
            pageInfo.put("size", size);
            pageInfo.put("total", total);
            pageInfo.put("totalPages", totalPages);

            Map<String, Object> data = new HashMap<>();
            data.put("items", items);

            out.put("ok", true);
            out.put("message", "");
            out.put("data", data);
            out.put("page", pageInfo);

        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            out.put("data", null);
            out.put("page", null);
        }

        JsonUtil.writeJson(resp, out);
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
