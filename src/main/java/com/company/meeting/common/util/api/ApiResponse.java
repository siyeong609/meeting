package com.company.meeting.common.util.api;

/**
 * 공통 JSON 응답 Wrapper
 * - ok: 성공 여부
 * - message: 에러/안내 메시지
 * - data: 실제 데이터
 * - page: 페이징 메타(필요 시)
 */
public class ApiResponse<T> {

    private boolean ok;
    private String message;
    private T data;
    private Object page;

    public ApiResponse() {
    }

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.ok = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok(T data, Object page) {
        ApiResponse<T> r = new ApiResponse<>();
        r.ok = true;
        r.data = data;
        r.page = page;
        return r;
    }

    public static <T> ApiResponse<T> fail(String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.ok = false;
        r.message = message;
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Object getPage() {
        return page;
    }
}
