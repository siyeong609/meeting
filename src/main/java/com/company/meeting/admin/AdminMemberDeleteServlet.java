package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.dto.UserDTO;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 관리자 - 회원 다중 삭제
 *
 * POST /admin/members/delete
 * params:
 * - ids : "2,3,4" (쉼표 구분)
 *
 * 정책:
 * - ADMIN 계정은 삭제 불가(DAO에서 role=USER 조건)
 * - 로그인한 관리자 자신은 삭제 불가
 *
 * 응답:
 * - ok=true, data=삭제된 수(int)
 * - (참고로 skipped/failed는 message로 안내 가능)
 */
@WebServlet("/admin/members/delete")
public class AdminMemberDeleteServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            String idsParam = req.getParameter("ids");
            if (idsParam == null || idsParam.isBlank()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("삭제할 대상이 없습니다.")));
                return;
            }

            // 로그인 관리자 id (자기 자신 삭제 방지)
            HttpSession session = req.getSession(false);
            int adminId = 0;
            if (session != null) {
                Object o = session.getAttribute("LOGIN_ADMIN");
                if (o instanceof UserDTO) {
                    adminId = ((UserDTO) o).getId();
                }
            }

            List<Integer> ids = parseIds(idsParam);

            UserService.DeleteResult result = userService.deleteMembers(ids, adminId);

            // 삭제 성공 count만 data로
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(result.getDeletedCount())));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }

    /**
     * "1,2,3" -> List<Integer>
     */
    private List<Integer> parseIds(String s) {
        String[] arr = s.split(",");
        List<Integer> ids = new ArrayList<>();
        for (String x : arr) {
            String v = x.trim();
            if (v.isBlank()) continue;
            try {
                ids.add(Integer.parseInt(v));
            } catch (Exception ignored) {
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 대상이 없습니다.");
        }
        return ids;
    }
}
