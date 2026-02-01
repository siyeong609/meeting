package com.company.meeting.admin.chat.service;

import com.company.meeting.admin.chat.dao.ChatThreadDAO;
import com.company.meeting.admin.chat.dto.AdminChatThreadListItem;

import java.util.List;

/**
 * ChatThreadService
 * - 스레드 목록(관리자용) 페이징/정렬/검색 처리
 *
 * 설계 포인트:
 * - 스레드 생성 정책이 "위젯 오픈 시 생성"이므로
 *   last_message_at이 NULL인 스레드가 존재할 수 있음
 * - 관리자 목록에서는 보통 "최근 대화 우선"이므로
 *   NULL(last_message_at)은 뒤로 보내는 정렬을 사용
 */
public class ChatThreadService {

    private final ChatThreadDAO dao = new ChatThreadDAO();

    /**
     * 스레드 목록 조회 (페이징)
     *
     * @param page   1-base page
     * @param size   page size (1~50 권장)
     * @param q      검색어(회원 아이디)
     * @param status OPEN/CLOSED/null(전체)
     */
    public ThreadPageResult listThreads(int page, int size, String q, String status) {
        if (page < 1) throw new IllegalArgumentException("page 값이 올바르지 않습니다.");
        if (size < 1 || size > 50) throw new IllegalArgumentException("size 값이 올바르지 않습니다.");

        int totalElements = dao.countThreads(q, status);

        int totalPages = (int) Math.ceil(totalElements / (double) size);
        if (totalPages < 1) totalPages = 1;

        // page가 totalPages를 넘어가면 마지막 페이지로 보정
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * size;

        List<AdminChatThreadListItem> items = dao.selectThreads(offset, size, q, status);

        return new ThreadPageResult(items, page, size, totalElements, totalPages);
    }

    /**
     * ThreadPageResult
     * - 컨트롤러(서블릿)에서 JSON 응답 포맷 구성하기 좋도록 결과 캡슐화
     */
    public static class ThreadPageResult {
        private final List<AdminChatThreadListItem> items;
        private final int page;
        private final int size;
        private final int totalElements;
        private final int totalPages;

        public ThreadPageResult(List<AdminChatThreadListItem> items,
                                int page,
                                int size,
                                int totalElements,
                                int totalPages) {
            this.items = items;
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public List<AdminChatThreadListItem> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public int getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }
}
