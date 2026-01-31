package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;

/**
 * 관리자 - 회원 정보 수정
 *
 * POST /admin/members/update
 * - application/x-www-form-urlencoded
 * - params:
 *   - userId (필수)
 *   - email (선택, 전달되면 업데이트. 빈값이면 null 처리)
 *   - newPassword (선택, 값이 있으면 해시 저장)
 *   - deleteProfile (선택, true면 프로필 삭제 처리)
 *
 * 프로필 삭제 처리:
 * - DB의 profile_image를 NULL로 만든다.
 * - 실제 파일도 삭제한다(업로드 폴더 내부 파일일 때만)
 * - default-profile.svg는 삭제 대상에서 제외한다.
 */
@WebServlet("/admin/members/update")
public class AdminMemberUpdateServlet extends HttpServlet {

    private final UserService userService = new UserService();

    // ✅ 네가 지정한 기본 프로필 경로(삭제 대상 제외)
    private static final String DEFAULT_PROFILE_DB_PATH = "/resources/uploads/profile/default-profile.svg";
    private static final String UPLOAD_BASE = "/resources/uploads/profile/";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            int userId = parseIntOrThrow(req.getParameter("userId"));

            String email = req.getParameter("email");             // null 가능
            String newPassword = req.getParameter("newPassword"); // null 가능
            String memo = req.getParameter("memo"); // null 가능
            boolean deleteProfile = parseBoolean(req.getParameter("deleteProfile"));

            // 1) 이메일 업데이트(전달되었을 때만)
            if (email != null) {
                userService.updateEmail(userId, email);
            }

            // 1) 이메일 업데이트(전달되었을 때만)
            if (memo != null) {
                userService.updateMemo(userId, memo);
            }

            // 2) 비밀번호 업데이트(입력되었을 때만)
            if (newPassword != null && !newPassword.isBlank()) {
                userService.changePassword(userId, newPassword);
            }

            // 3) 프로필 삭제(체크박스 기반)
            if (deleteProfile) {
                deleteProfileInternal(req, userId);
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok("ok")));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail(e.getMessage())));
        } catch (RuntimeException e) {
            // 이메일 UNIQUE 충돌 등 처리
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate")) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("이미 사용 중인 이메일입니다.")));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }

    /**
     * 프로필 삭제 로직(파일 + DB)
     */
    private void deleteProfileInternal(HttpServletRequest req, int userId) {

        // 현재 DB에 저장된 프로필 경로 조회
        String currentPath = userService.getProfileImagePath(userId);

        // DB NULL 처리
        userService.clearProfileImage(userId);

        // 파일 삭제 조건:
        // - 경로가 비어있지 않아야 함
        // - default 프로필은 삭제하면 안 됨
        // - 업로드 폴더 내부 파일만 삭제 허용(경로 안전장치)
        if (currentPath == null || currentPath.isBlank()) return;
        if (DEFAULT_PROFILE_DB_PATH.equals(currentPath)) return;
        if (!currentPath.startsWith(UPLOAD_BASE)) return;

        // 실제 파일 경로로 변환 후 삭제 시도
        String realPath = req.getServletContext().getRealPath(currentPath);
        if (realPath == null || realPath.isBlank()) return;

        File f = new File(realPath);
        if (f.exists()) {
            // 삭제 실패해도 DB는 이미 NULL 처리되었으니 시스템은 정상 동작
            // (원인은 권한/잠금/경로 문제일 수 있음)
            f.delete();
        }
    }

    /**
     * userId 파라미터 안전 파싱
     */
    private int parseIntOrThrow(String s) {
        try {
            if (s == null || s.isBlank()) throw new IllegalArgumentException("userId가 없습니다.");
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("userId 형식이 올바르지 않습니다.");
        }
    }

    /**
     * deleteProfile 파라미터 파싱
     * - "true", "1", "on" 등을 true로 처리
     */
    private boolean parseBoolean(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
    }
}
