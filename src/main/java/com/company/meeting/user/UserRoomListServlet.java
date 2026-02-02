package com.company.meeting.user.service;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.dto.RoomListItem;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserRoomListServlet
 * - GET  /user/rooms : 목록 JSP forward
 * - POST /user/rooms : 목록 JSON 응답 (Ajax 렌더링)
 *
 * 응답 포맷(권장):
 * ApiResponse.ok({ items: [...] }, { page, size, totalPages, total })
 */
@WebServlet(name = "UserRoomListServlet", urlPatterns = {"/user/rooms"})
public class UserRoomListServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ✅ 화면만 forward (데이터는 JS에서 POST로 받음)
        req.getRequestDispatcher("/WEB-INF/views/user/room/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // ✅ 파라미터 파싱 (admin 패턴과 동일)
            int page = parseInt(req.getParameter("page"), 1);
            int size = parseInt(req.getParameter("size"), 10);
            String q = safeTrim(req.getParameter("q"));

            // 방어: page/size 최소값
            if (page < 1) page = 1;
            if (size < 1) size = 10;
            if (size > 50) size = 50;

            // ✅ 사용자용은 활성 회의실만 (RoomService에 메서드 추가했으면 이걸 사용)
            int total = roomService.countActiveRooms(q);
            int totalPages = (int) Math.ceil(total / (double) size);
            if (totalPages < 1) totalPages = 1;
            if (page > totalPages) page = totalPages;

            List<RoomListItem> items = roomService.listActiveRooms(q, page, size);

            // data: { items: [...] }
            Map<String, Object> data = new HashMap<>();
            data.put("items", items);

            // page meta: { page, size, totalPages, total }
            Map<String, Object> pageMeta = new HashMap<>();
            pageMeta.put("page", page);
            pageMeta.put("size", size);
            pageMeta.put("totalPages", totalPages);
            pageMeta.put("total", total);

            JsonUtil.writeJson(resp, ApiResponse.ok(data, pageMeta));

        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage()));

        } catch (SQLException e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail("DB 오류가 발생했습니다."));

        } catch (Exception e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail("서버 오류가 발생했습니다."));
        }
    }

    private int parseInt(String s, int def) {
        try {
            if (s == null) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }
}
