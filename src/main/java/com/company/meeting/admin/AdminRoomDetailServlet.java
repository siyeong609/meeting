package com.company.meeting.admin;

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
 * AdminRoomDetailServlet
 * - GET  /admin/rooms/detail?id=1 : (관리자) 회의실 상세/예약현황 JSP forward
 * - POST /admin/rooms/detail      : (관리자) JSON 회의실 상세 데이터(Ajax)
 *
 * ✅ 관리자 정책:
 * - 사용자와 다르게 "비활성 회의실"도 관리 대상이므로
 *   상세 조회는 막지 않는다(단, 존재하지 않으면 404)
 *
 * ✅ detail.jsp는 "예약현황 중심" 페이지여도,
 *   상단에 회의실 기본정보 카드가 필요하니 이 API가 반드시 필요함.
 */
@WebServlet(name = "AdminRoomDetailServlet", urlPatterns = {"/admin/rooms/detail"})
public class AdminRoomDetailServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ✅ 관리자 로그인 체크(프로젝트 규약)
        Object loginAdmin = req.getSession().getAttribute("LOGIN_ADMIN");
        if (loginAdmin == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/login");
            return;
        }

        // ✅ detail 페이지는 roomId를 JSP에서 쓸 수 있게 전달
        String id = req.getParameter("id");
        req.setAttribute("roomId", id);

        // ✅ 여기 경로는 네 프로젝트 admin detail.jsp 위치에 맞춰 조정해야 함
        // 예: /WEB-INF/views/admin/room/detail.jsp
        req.getRequestDispatcher("/WEB-INF/views/admin/room/detail.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // ✅ 관리자 로그인 체크(ajax)
            Object loginAdmin = req.getSession().getAttribute("LOGIN_ADMIN");
            if (loginAdmin == null) {
                resp.setStatus(401);
                JsonUtil.writeJson(resp, ApiResponse.fail("관리자 로그인이 필요합니다."));
                return;
            }

            int id = parseInt(req.getParameter("id"), 0);
            if (id <= 0) {
                resp.setStatus(400);
                JsonUtil.writeJson(resp, ApiResponse.fail("회의실 ID가 올바르지 않습니다."));
                return;
            }

            RoomDetail d = roomService.getRoomDetail(id);
            if (d == null) {
                resp.setStatus(404);
                JsonUtil.writeJson(resp, ApiResponse.fail("회의실을 찾을 수 없습니다."));
                return;
            }

            // ✅ 관리자: 비활성도 보여줌 (숨기지 않음)
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
