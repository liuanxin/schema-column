package com.github.liuanxin.query.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.liuanxin.query.model.ReqResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryJsonUtil {

    private static final TypeReference<Map<String, ReqResult>> INNER_RESULT_TYPE = new TypeReference() {};
    private static final TypeReference<Map<String, List<String>>> DATE_FORMAT_RESULT_TYPE = new TypeReference() {};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        } else {
            try {
                return OBJECT_MAPPER.writeValueAsString(obj);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    public static <S,T> T convert(S source, Class<T> clazz) {
        String json = toJson(source);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static <S,T> T convertType(S source, TypeReference<T> type) {
        String json = toJson(source);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException ignore) {
            return null;
        }
    }

    public static Map<String, ReqResult> convertInnerResult(Object obj) {
        return convertType(obj, INNER_RESULT_TYPE);
    }

    public static Map<String, List<String>> convertDateResult(Object obj) {
        return convertType(obj, DATE_FORMAT_RESULT_TYPE);
    }
}
