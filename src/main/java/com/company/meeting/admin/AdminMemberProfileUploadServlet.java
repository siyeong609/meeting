package com.company.meeting.admin;

import com.company.meeting.common.util.api.ApiResponse;
import com.company.meeting.common.util.json.JsonUtil;
import com.company.meeting.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 관리자 - 회원 프로필 이미지 업로드
 *
 * POST /admin/members/profile/upload
 * - multipart/form-data
 * - fields:
 *   - userId: int
 *   - profile: file (jpg/jpeg/png, <= 5MB)
 *
 * 저장 위치(프로젝트 내부):
 *  /resources/uploads/profile/{userId}.{ext}
 *
 * DB 저장값:
 *  /resources/uploads/profile/{userId}.{ext}
 *
 * 주의:
 * - Smart Tomcat이 src/main/webapp 을 배포 디렉터리로 쓰는 현재 개발 환경에서 안전하게 동작
 * - 운영에서 WAR 재배포 시 유실될 수 있으므로, 운영 전에는 외부 디렉터리로 분리 권장
 */
@WebServlet("/admin/members/profile/upload")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,       // 1MB 메모리 임계
        maxFileSize = 5L * 1024 * 1024,         // 5MB
        maxRequestSize = 6L * 1024 * 1024       // 6MB
)
public class AdminMemberProfileUploadServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try {
            // 1) userId 파싱
            int userId = parseIntOrThrow(req.getParameter("userId"));

            // 2) 파일 파트 읽기
            Part filePart = req.getPart("profile");
            if (filePart == null || filePart.getSize() <= 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("profile 파일이 없습니다.")));
                return;
            }

            // 3) 검증: 용량
            long size = filePart.getSize();
            if (size > 5L * 1024 * 1024) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("파일 크기는 5MB 이하여야 합니다.")));
                return;
            }

            // 4) 확장자 판단 (파일명 기반 + 최소한의 안전 검사)
            String submitted = filePart.getSubmittedFileName();
            String ext = getExtensionLower(submitted);

            // jpeg -> jpg 통일
            if ("jpeg".equals(ext)) ext = "jpg";

            if (!("jpg".equals(ext) || "png".equals(ext))) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("JPG 또는 PNG 파일만 업로드 가능합니다.")));
                return;
            }

            // 5) 저장 경로 계산
            // webapp 기준 실제 파일시스템 경로
            String relativeDir = "/resources/uploads/profile";
            String realDirPath = getServletContext().getRealPath(relativeDir);

            if (realDirPath == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("서버 저장 경로를 확인할 수 없습니다.")));
                return;
            }

            File dir = new File(realDirPath);
            if (!dir.exists()) {
                boolean ok = dir.mkdirs();
                if (!ok) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("업로드 폴더 생성 실패")));
                    return;
                }
            }

            // 6) 파일명 규칙: userId.ext (단순/명확)
            String fileName = userId + "." + ext;
            File savedFile = new File(dir, fileName);

            // 7) 저장(덮어쓰기)
            try (InputStream in = filePart.getInputStream()) {
                Files.copy(in, savedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 8) DB 저장 경로(URL)
            String dbPath = "/resources/uploads/profile/" + fileName;
            userService.updateProfileImage(userId, dbPath);

            // 9) 응답
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(dbPath)));

        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail(e.getMessage())));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(ApiResponse.fail("internal server error")));
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
     * 파일 확장자 추출(소문자)
     */
    private String getExtensionLower(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx + 1).trim().toLowerCase();
    }
}
