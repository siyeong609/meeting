package com.company.meeting.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * AdminRoomDetailPageServlet
 * - GET /admin/detail?id=123
 * - 목적: "예약 현황(상세)" 페이지 진입(뼈대 렌더)
 * - 데이터는 JS가 POST API(/admin/rooms/detail, /admin/rooms/status/*)로 로드하는 구조 권장
 */
@WebServlet("/admin/detail")
public class AdminRoomDetailPageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // ✅ 쿼리 파라미터: id (room id)
        String idRaw = req.getParameter("id");
        int roomId = 0;

        try {
            roomId = Integer.parseInt(idRaw == null ? "0" : idRaw.trim());
        } catch (Exception ignore) {
            roomId = 0;
        }

        if (roomId <= 0) {
            // ✅ 잘못된 접근이면 목록으로 돌림
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
            return;
        }

        // ✅ JSP에서 pageTitle / pageCss / roomId 를 사용할 수 있도록 주입
        req.setAttribute("pageTitle", "회의실 예약 현황");
        req.setAttribute("pageCss", "room.css");
        req.setAttribute("roomId", roomId);

        req.getRequestDispatcher("/WEB-INF/views/admin/room/detail.jsp").forward(req, resp);
    }
}
