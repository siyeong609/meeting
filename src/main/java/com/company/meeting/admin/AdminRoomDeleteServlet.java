package com.company.meeting.admin;

import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /admin/rooms/delete
 * - JS(list.js) 기준 규약:
 *   - POST body: ids=1,2,3  (선택삭제)
 *   - 응답: { ok:true, data: 삭제된개수(int) }
 *
 * - 호환: id=1 단건 삭제도 지원
 */
@WebServlet(urlPatterns = "/admin/rooms/delete")
public class AdminRoomDeleteServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Map<String, Object> out = new HashMap<>();

        try {
            String idsParam = trimToNull(req.getParameter("ids"));
            String idParam = trimToNull(req.getParameter("id"));

            int deletedCount;

            if (idsParam != null) {
                List<Integer> ids = parseIds(idsParam);
                deletedCount = roomService.deleteMany(ids);
            } else if (idParam != null) {
                int id = parseInt(idParam, 0);
                deletedCount = roomService.delete(id) ? 1 : 0;
            } else {
                throw new IllegalArgumentException("삭제 파라미터(ids 또는 id)가 없습니다.");
            }

            out.put("ok", true);
            out.put("message", "삭제 완료");
            out.put("data", deletedCount); // ✅ JS가 기대하는 형태: number
            out.put("page", null);

        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            out.put("data", null);
            out.put("page", null);
        }

        JsonUtil.writeJson(resp, out);
    }

    private static List<Integer> parseIds(String csv) {
        List<Integer> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            int v = parseInt(part.trim(), 0);
            if (v > 0) ids.add(v);
        }
        return ids;
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
