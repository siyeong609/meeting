package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.room.dto.RoomDetail;
import com.company.meeting.room.service.RoomService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * UserRoomDetailDataServlet
 * - POST /user/rooms/detail-data : 회의실 상세(JSON, 운영시간 포함)
 * 파라미터:
 * - id
 *
 * ※ detail.jsp / new.jsp에서 공용으로 사용 가능
 */
@WebServlet("/user/rooms/detail-data")
public class UserRoomDetailDataServlet extends HttpServlet {

    private final RoomService roomService = new RoomService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            int id = parseInt(req.getParameter("id"), 0);
            if (id <= 0) {
                JsonUtil.writeJson(resp, ApiResponse.fail("회의실 ID가 올바르지 않습니다."));
                return;
            }

            RoomDetail d = roomService.getRoomDetail(id);
            if (d == null) {
                JsonUtil.writeJson(resp, ApiResponse.fail("회의실을 찾을 수 없습니다."));
                return;
            }

            JsonUtil.writeJson(resp, ApiResponse.ok(d));

        } catch (Exception e) {
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() != null ? e.getMessage() : "상세 조회 실패"));
        }
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v); } catch (Exception ignore) { return def; }
    }
}
