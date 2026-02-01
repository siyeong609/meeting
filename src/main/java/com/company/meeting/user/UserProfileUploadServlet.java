package com.company.meeting.user;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.dto.UserDTO;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * 회원 프로필 업로드 API
 *
 * URL:
 * - POST /user/profile/upload (multipart/form-data)
 *
 * 입력:
 * - profile: 이미지 파일 (jpg/jpeg/png), 최대 5MB
 *
 * 처리:
 * - 파일 저장: /resources/uploads/profile/
 * - DB 갱신: user.profile_image = "/resources/uploads/profile/{filename}"
 * - 기존 커스텀 파일이 있으면 삭제 시도
 *
 * 보안:
 * - userId 파라미터를 받지 않고, 세션 LOGIN_USER의 id로만 처리한다.
 */
@WebServlet("/user/profile/upload")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5L * 1024 * 1024,
        maxRequestSize = 6L * 1024 * 1024
)
public class UserProfileUploadServlet extends HttpServlet {

    private static final String DEFAULT_PROFILE_DB_PATH = "/resources/uploads/profile/default-profile.svg";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            HttpSession session = req.getSession(false);
            UserDTO loginUser = (session == null) ? null : (UserDTO) session.getAttribute("LOGIN_USER");

            if (loginUser == null) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("로그인이 필요합니다.")));
                return;
            }

            long userId = extractUserPk(loginUser);

            Part part = req.getPart("profile");
            if (part == null || part.getSize() <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("업로드 파일이 없습니다.")));
                return;
            }

            // ✅ 확장자/타입 검증
            String submitted = part.getSubmittedFileName();
            String ext = getExt(submitted);
            if (!("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext))) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("JPG 또는 PNG 파일만 업로드 가능합니다.")));
                return;
            }

            String contentType = (part.getContentType() == null) ? "" : part.getContentType().toLowerCase();
            if (!(contentType.contains("jpeg") || contentType.contains("jpg") || contentType.contains("png"))) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("이미지 파일만 업로드 가능합니다.")));
                return;
            }

            // ✅ 저장 경로 계산
            String uploadDirReal = getServletContext().getRealPath("/resources/uploads/profile");
            if (uploadDirReal == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("업로드 경로를 확인할 수 없습니다.")));
                return;
            }

            File dir = new File(uploadDirReal);
            if (!dir.exists()) dir.mkdirs();

            String fileName = "u" + userId + "_" + System.currentTimeMillis() + "." + ext;
            Path target = Paths.get(uploadDirReal, fileName);

            // ✅ 파일 저장
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            // ✅ DB 저장 경로
            String dbPath = "/resources/uploads/profile/" + fileName;

            // ✅ 기존 커스텀 파일 삭제(가능하면)
            String oldProfile = tryGetString(loginUser, "getProfileImage");
            if (oldProfile == null) oldProfile = tryGetString(loginUser, "getProfile_image");

            deleteOldCustomProfileIfPossible(oldProfile, uploadDirReal);

            // ✅ DB 업데이트(profile_image)
            updateProfileImageInDb(userId, dbPath);

            // ✅ 세션에도 반영(가능하면)
            trySet(loginUser, "setProfileImage", String.class, dbPath);
            trySet(loginUser, "setProfile_image", String.class, dbPath);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(dbPath)));

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
        }
    }

    private void updateProfileImageInDb(long userId, String dbPath) throws Exception {
        // ✅ schema.sql 기준: user.profile_image + updated_at 존재
        String sql = "UPDATE user SET profile_image = ?, updated_at = NOW() WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, dbPath);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    private void deleteOldCustomProfileIfPossible(String oldProfileDbPath, String uploadDirReal) {
        // oldProfileDbPath가 없거나 default면 삭제하지 않는다.
        if (oldProfileDbPath == null) return;

        String p = oldProfileDbPath.trim();
        if (p.isEmpty()) return;
        if (DEFAULT_PROFILE_DB_PATH.equals(p)) return;

        // "/resources/uploads/profile/..." 만 삭제 대상으로 인정(안전)
        if (!p.startsWith("/resources/uploads/profile/")) return;

        try {
            String oldReal = getServletContext().getRealPath(p);
            if (oldReal == null) return;

            Path uploadDir = Paths.get(uploadDirReal).toRealPath();
            Path oldPath = Paths.get(oldReal).toRealPath();

            // ✅ 업로드 디렉터리 내부 파일만 삭제
            if (!oldPath.startsWith(uploadDir)) return;

            Files.deleteIfExists(oldPath);
        } catch (Exception ignore) {
            // 파일 삭제 실패는 치명적이지 않으므로 무시 (DB/업로드 성공이 우선)
        }
    }

    private String getExt(String filename) {
        if (filename == null) return "";
        String f = filename.trim().toLowerCase();
        int idx = f.lastIndexOf(".");
        if (idx < 0) return "";
        return f.substring(idx + 1);
    }

    private long extractUserPk(UserDTO loginUser) {
        Object v = tryInvoke(loginUser, "getId");
        if (v == null) v = tryInvoke(loginUser, "getUserPk");
        if (v == null) v = tryInvoke(loginUser, "getUserNo");

        if (v == null) {
            throw new IllegalStateException("세션 사용자 PK를 찾을 수 없습니다. (UserDTO에 getId() 필요)");
        }

        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private Object tryInvoke(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            return m.invoke(obj);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String tryGetString(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object v = m.invoke(obj);
            return v == null ? null : String.valueOf(v);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void trySet(Object obj, String methodName, Class<?> argType, Object value) {
        try {
            Method m = obj.getClass().getMethod(methodName, argType);
            m.invoke(obj, value);
        } catch (Exception ignore) {
        }
    }
}
