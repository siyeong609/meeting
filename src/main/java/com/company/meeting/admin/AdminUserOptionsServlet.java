package com.company.meeting.admin;

import com.company.meeting.admin.dto.UserOption;
import com.company.meeting.common.db.DBConnection;
import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminUserOptionsServlet
 * - POST /admin/users/options
 * - params: q(optional)
 * - return: [{id, name}, ...]
 *
 * ✅ role='USER'만 내려줌 (대리예약 대상)
 */
@WebServlet("/admin/users/options")
public class AdminUserOptionsServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Object loginAdmin = req.getSession().getAttribute("LOGIN_ADMIN");
            if (loginAdmin == null) {
                resp.setStatus(401);
                JsonUtil.writeJson(resp, ApiResponse.fail("관리자 로그인이 필요합니다."));
                return;
            }

            String q = req.getParameter("q");
            List<UserOption> list = findUsers(q);

            JsonUtil.writeJson(resp, ApiResponse.ok(list));

        } catch (Exception e) {
            resp.setStatus(500);
            JsonUtil.writeJson(resp, ApiResponse.fail(e.getMessage() == null ? "유저 목록 조회 실패" : e.getMessage()));
        }
    }

    private List<UserOption> findUsers(String q) throws Exception {
        boolean hasQ = (q != null && !q.trim().isEmpty());

        String sql = ""
                + "SELECT id, name "
                + "FROM user "
                + "WHERE role='USER' "
                + (hasQ ? "AND (name LIKE ? OR login_id LIKE ?) " : "")
                + "ORDER BY id DESC "
                + "LIMIT 200";

        List<UserOption> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (hasQ) {
                String like = "%" + q.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new UserOption(rs.getInt("id"), rs.getString("name")));
                }
            }
        }

        return list;
    }
}
