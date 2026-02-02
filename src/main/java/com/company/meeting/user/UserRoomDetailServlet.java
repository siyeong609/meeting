package com.company.meeting.user.service;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.dto.RoomDetail;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * UserRoomDetailServlet
 * - GET  /user/rooms/detail?id=1 : 상세 JSP forward
 * - POST /user/rooms/detail      : JSON 상세 데이터 응답(Ajax)
 *
 * 정책:
 * - 사용자 화면은 기본적으로 "활성 회의실"만 노출이 자연스러움
 * - 상세도 비활성은 "없음" 처리하는 게 UX/보안상 깔끔
 */
@WebServlet(name = "UserRoomDetailServlet", urlPatterns = {"/user/rooms/detail"})
public class UserRoomDetailServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ✅ 상세 페이지는 roomId를 JSP에서 쓸 수 있게 전달
        String id = req.getParameter("id");
        req.setAttribute("roomId", id);
        req.getRequestDispatcher("/WEB-INF/views/user/room/detail.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = parseInt(req.getParameter("id"), 0);
            if (id <= 0) {
                throw new IllegalArgumentException("회의실 ID가 올바르지 않습니다.");
            }

            RoomDetail d = roomService.getRoomDetail(id);
            if (d == null) {
                resp.setStatus(404);
                JsonUtil.writeJson(resp, ApiResponse.fail("회의실을 찾을 수 없습니다."));
                return;
            }

            // ✅ 사용자 정책: 비활성이면 상세도 숨김
            if (!d.isActive()) {
                resp.setStatus(404);
                JsonUtil.writeJson(resp, ApiResponse.fail("회의실을 찾을 수 없습니다."));
                return;
            }

            JsonUtil.writeJson(resp, ApiResponse.ok(d));

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
}
