package com.company.meeting.common.util.paging;

/**
 * 페이징 요청 공통 DTO
 * - page: 1부터 시작
 * - size: 한 페이지당 개수
 */
public class PageRequest {

    private final int page;
    private final int size;

    /**
     * @param page 1부터 시작
     * @param size 1 이상
     */
    public PageRequest(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        this.page = page;
        this.size = size;
    }

    /**
     * LIMIT/OFFSET 계산용 offset
     */
    public int getOffset() {
        return (page - 1) * size;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
