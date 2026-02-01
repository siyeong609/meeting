package com.company.meeting.user;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.dto.UserDTO;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * 회원 내정보 저장 API
 *
 * URL:
 * - POST /user/profile/update
 *
 * 기능:
 * - email 변경(빈값이면 NULL 저장)
 * - 비밀번호 변경(입력된 경우만)
 * - 프로필 삭제(deleteProfile=true인 경우)
 *
 * 보안:
 * - userId 파라미터를 받지 않고, 세션 LOGIN_USER의 id로만 처리한다.
 */
@WebServlet("/user/profile/update")
public class UserProfileUpdateServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            // ✅ 로그인 사용자 확인
            HttpSession session = req.getSession(false);
            UserDTO loginUser = (session == null) ? null : (UserDTO) session.getAttribute("LOGIN_USER");

            if (loginUser == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("로그인이 필요합니다.")));
                return;
            }

            // ✅ PK를 int로 확정 (UserService가 int를 받는 구조이므로)
            int userId = extractUserPkAsInt(loginUser);

            // ✅ 입력값
            String emailRaw = nvl(req.getParameter("email"));
            String newPassword = nvl(req.getParameter("newPassword"));
            String deleteProfileStr = nvl(req.getParameter("deleteProfile"));

            boolean deleteProfile = "true".equalsIgnoreCase(deleteProfileStr);

            // ✅ email: 빈값이면 NULL로 저장(= 이메일 제거)
            String emailOrNull = emailRaw.isEmpty() ? null : emailRaw;

            /*
             * ✅ 변경 작업 수행
             * - email은 빈값도 "지우기" 의미가 있으므로 항상 반영
             * - password는 입력된 경우만
             * - deleteProfile은 true일 때만
             */
            userService.updateEmail(userId, emailOrNull);
            // 세션 객체에도 반영(가능하면)
            trySet(loginUser, "setEmail", String.class, emailOrNull);

            if (!newPassword.isEmpty()) {
                userService.changePassword(userId, newPassword);
            }

            if (deleteProfile) {
                userService.clearProfileImage(userId);
                // 세션 객체에도 반영(가능하면)
                trySet(loginUser, "setProfileImage", String.class, null);
                trySet(loginUser, "setProfile_image", String.class, null);
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("ok")));

        } catch (IllegalStateException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }

    /**
     * null-safe trim
     */
    private String nvl(String s) {
        return (s == null) ? "" : s.trim();
    }

    /**
     * ✅ 세션 UserDTO에서 PK(id)를 int로 추출
     *
     * - UserService가 int를 받는 구조라서 여기에서 int로 확정한다.
     * - long 범위를 넘어가면 예외(데이터 이상)로 처리한다.
     */
    private int extractUserPkAsInt(UserDTO loginUser) {
        Object v = tryInvoke(loginUser, "getId");      // 가장 일반적
        if (v == null) v = tryInvoke(loginUser, "getUserPk");
        if (v == null) v = tryInvoke(loginUser, "getUserNo");

        if (v == null) {
            throw new IllegalStateException("세션 사용자 PK를 찾을 수 없습니다. (UserDTO에 getId() 필요)");
        }

        long idLong;

        // 숫자면 그대로
        if (v instanceof Number) {
            idLong = ((Number) v).longValue();
        } else {
            // 문자열이면 파싱
            try {
                idLong = Long.parseLong(String.valueOf(v));
            } catch (Exception ex) {
                throw new IllegalStateException("세션 사용자 PK 형식이 올바르지 않습니다.");
            }
        }

        // ✅ int 범위 체크
        if (idLong > Integer.MAX_VALUE || idLong < Integer.MIN_VALUE) {
            throw new IllegalStateException("세션 사용자 PK가 int 범위를 초과했습니다.");
        }

        return (int) idLong;
    }

    /**
     * 리플렉션 안전 호출
     */
    private Object tryInvoke(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 세션 DTO에 값 반영 시도 (setter 없으면 무시)
     */
    private void trySet(Object obj, String methodName, Class<?> argType, Object value) {
        try {
            Method m = obj.getClass().getMethod(methodName, argType);
            m.invoke(obj, value);
        } catch (Exception ignore) {
            // setter가 없어도 기능은 동작해야 하므로 무시
        }
    }
}
