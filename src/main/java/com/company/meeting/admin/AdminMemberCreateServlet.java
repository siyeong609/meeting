package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 관리자 - 회원 생성
 *
 * POST /admin/members/create
 * params:
 * - loginId
 * - password
 * - name
 * - email (optional)
 * - role (optional: USER/ADMIN, default USER)
 * - memo (optional) ✅ 추가
 *
 * 응답:
 * - ok=true, data=생성된 userId(int)
 */
@WebServlet("/admin/members/create")
public class AdminMemberCreateServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // ✅ JSON 응답 고정
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            // ✅ 파라미터 수집
            String loginId = req.getParameter("loginId");
            String password = req.getParameter("password");
            String name = req.getParameter("name");
            String email = req.getParameter("email");
            String role = req.getParameter("role");
            String memo = req.getParameter("memo"); // ✅ 메모 추가

            // ✅ 생성(기존 서비스 시그니처 유지)
            int createdId = userService.createMember(loginId, password, name, email, role);

            // ✅ 메모는 생성 직후 저장 (memo가 비어있으면 NULL 처리 로직은 DAO에서 처리됨)
            if (memo != null && !memo.isBlank()) {
                userService.updateMemo(createdId, memo);
            }

            // ✅ OK 응답
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(createdId)));

        } catch (IllegalArgumentException e) {
            // ✅ 입력값 검증 실패(필수값 누락 등)
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail(e.getMessage())));

        } catch (RuntimeException e) {
            // ✅ duplicate login_id / email 등
            // - 여기서 e.getMessage()로 분기하면 DB/드라이버마다 메시지가 달라질 수 있으니 고정 문구 처리
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("중복된 아이디/이메일일 수 있습니다.")));

        } catch (Exception e) {
            // ✅ 그 외 서버 에러
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }
}
