package com.company.meeting.admin;

import com.company.meeting.admin.chat.service.ChatThreadService;
import com.company.meeting.admin.chat.service.ChatThreadService.ThreadPageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AdminChatThreadsServlet
 * - 관리자 대화관리(채팅문의) 스레드 목록 API
 *
 * URL:
 * - POST /admin/chat/threads
 *
 * Params (x-www-form-urlencoded):
 * - page: int (default 1)
 * - size: int (default 10, max 50)
 * - q:    string (회원 아이디 검색)
 * - status: string ("OPEN" | "CLOSED" | "" 전체)
 *
 * Response JSON (프로젝트 공통 포맷):
 * {
 *   ok: true/false,
 *   message: "...",
 *   data: { items: [...] },
 *   page: { page, size, totalElements, totalPages }
 * }
 */
@WebServlet("/admin/chat/threads")
public class AdminChatThreadsServlet extends HttpServlet {

    private static final ObjectMapper OM = new ObjectMapper();

    // ✅ Service 분리 (DAO 직접 호출보다 유지보수 유리)
    private final ChatThreadService threadService = new ChatThreadService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");

        // -------------------------
        // 1) 파라미터 파싱/검증
        // -------------------------
        int page = parseIntOrDefault(request.getParameter("page"), 1);
        int size = parseIntOrDefault(request.getParameter("size"), 10);

        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 50) size = 50;

        String q = trimToNull(request.getParameter("q"));
        String status = trimToNull(request.getParameter("status"));

        // status는 OPEN/CLOSED/NULL만 허용(그 외는 무시)
        if (status != null && !(status.equals("OPEN") || status.equals("CLOSED"))) {
            status = null;
        }

        // -------------------------
        // 2) 목록 조회
        // -------------------------
        try {
            ThreadPageResult result = threadService.listThreads(page, size, q, status);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", result.getItems());

            Map<String, Object> pageObj = new LinkedHashMap<>();
            pageObj.put("page", result.getPage());
            pageObj.put("size", result.getSize());
            pageObj.put("totalElements", result.getTotalElements());
            pageObj.put("totalPages", result.getTotalPages());

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("ok", true);
            root.put("message", "");
            root.put("data", data);
            root.put("page", pageObj);

            response.setStatus(200);
            OM.writeValue(response.getWriter(), root);

        } catch (IllegalArgumentException e) {
            // ✅ 클라이언트 입력 문제
            response.setStatus(400);
            OM.writeValue(response.getWriter(), errorBody(e.getMessage()));

        } catch (Exception e) {
            // ✅ 서버 오류 (fetchJson이 body를 읽을 수 있도록 JSON은 항상 내려준다)
            response.setStatus(500);
            OM.writeValue(response.getWriter(), errorBody("스레드 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // -------------------------
    // 유틸
    // -------------------------
    private static int parseIntOrDefault(String s, int def) {
        try {
            if (s == null) return def;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Map<String, Object> errorBody(String message) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("ok", false);
        root.put("message", message == null ? "오류" : message);
        root.put("data", new LinkedHashMap<>());
        root.put("page", new LinkedHashMap<>());
        return root;
    }
}
