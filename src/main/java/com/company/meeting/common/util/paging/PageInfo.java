package com.company.meeting.common.util.paging;

/**
 * 페이징 응답 메타 정보
 */
public class PageInfo {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PageInfo(int page, int size, long totalElements) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        this.page = page;
        this.size = size;
        this.totalElements = Math.max(totalElements, 0);

        // 총 페이지 계산 (ceil)
        this.totalPages = (int) ((this.totalElements + size - 1) / size);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
