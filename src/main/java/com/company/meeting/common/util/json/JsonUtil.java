package com.company.meeting.common.util.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;

/**
 * 공통 JSON 유틸
 * - ObjectMapper는 무겁기 때문에 싱글턴처럼 재사용
 * - Servlet에서 JSON 응답을 쉽게 내려주기 위한 writeJson 제공
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            // 날짜/시간을 타임스탬프가 아닌 문자열로 내려주고 싶으면 설정을 추가할 수 있음
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {
    }

    /**
     * 객체를 JSON 문자열로 직렬화한다.
     *
     * @param obj 직렬화 대상
     * @return JSON 문자열
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }

    /**
     * HttpServletResponse에 JSON으로 응답한다.
     * - content-type과 charset을 강제 설정
     * - writer로 JSON 문자열을 출력
     *
     * @param resp HttpServletResponse
     * @param obj  JSON으로 내려줄 객체(Map/DTO 등)
     */
    public static void writeJson(HttpServletResponse resp, Object obj) {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        String json = toJson(obj);

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(json);
            writer.flush();
        } catch (Exception e) {
            // 응답 쓰기 실패는 여기서도 RuntimeException으로 처리
            throw new RuntimeException("JSON 응답 쓰기 실패", e);
        }
    }
}
