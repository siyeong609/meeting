package com.company.meeting.common.util.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 공통 JSON 유틸
 * - ObjectMapper는 무겁기 때문에 싱글턴처럼 재사용
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            // 날짜/시간을 타임스탬프가 아닌 문자열로 내려주고 싶으면 설정을 추가할 수 있음
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }
}
