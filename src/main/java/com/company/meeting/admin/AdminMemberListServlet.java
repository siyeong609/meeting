package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.common.util.paging.PageRequest;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 관리자 회원 관리
 * <p>
 * GET  /admin/members : 화면(JSP)
 * POST /admin/members : JSON 데이터 (page/size/q 지원)
 */
@WebServlet("/admin/members")
public class AdminMemberListServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/admin/member/list.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            int page = parseIntOrDefault(req.getParameter("page"), 1);
            int size = parseIntOrDefault(req.getParameter("size"), 10);
            String q = req.getParameter("q");

            // size 상한 제한(남용 방지)
            if (size > 100) size = 100;

            PageRequest pr = new PageRequest(page, size);

            UserService.AdminMemberPageResult result =
                    userService.getAdminMembers(q, pr);

            // 공통 응답 포맷: ok + data + page
            ApiResponse<Object> response = ApiResponse.ok(result.getData(), result.getPage());

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(response));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }

    /**
     * 숫자 파싱 공통 처리
     */
    private int parseIntOrDefault(String s, int def) {
        try {
            if (s == null || s.isBlank()) return def;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
