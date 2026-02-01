package com.company.meeting.admin;

import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.dto.RoomDetail;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * /admin/rooms/detail
 * - JS(list.js) 기준 규약:
 *   - POST로 호출
 *   - body: id=1
 *   - 응답: { ok:true, message:"", data: RoomDetail, page:null }
 */
@WebServlet(urlPatterns = "/admin/rooms/detail")
public class AdminRoomDetailServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        int id = parseInt(req.getParameter("id"), 0);

        Map<String, Object> out = new HashMap<>();
        try {
            if (id <= 0) {
                throw new IllegalArgumentException("회의실 ID가 올바르지 않습니다.");
            }

            RoomDetail detail = roomService.getRoomDetail(id);
            if (detail == null) {
                out.put("ok", false);
                out.put("message", "회의실을 찾을 수 없습니다.");
                out.put("data", null);
                out.put("page", null);
                JsonUtil.writeJson(resp, out);
                return;
            }

            out.put("ok", true);
            out.put("message", "");
            out.put("data", detail); // ✅ JS가 기대하는 형태: data 자체가 RoomDetail
            out.put("page", null);

        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            out.put("data", null);
            out.put("page", null);
        }

        JsonUtil.writeJson(resp, out);
    }

    // (선택) 브라우저에서 편하게 테스트하려면 GET도 POST로 위임
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
