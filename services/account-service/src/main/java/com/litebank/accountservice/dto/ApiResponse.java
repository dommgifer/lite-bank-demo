package com.litebank.accountservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Boolean success;
    private T data;
    private ErrorDetails error;
    private String traceId;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .error(null)
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorDetails error, String traceId) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(null)
                .error(error)
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private String code;
        private String type;
        private String message;
        private String category;
        private Map<String, String> details;
        private String traceId;
        private String timestamp;
    }
}
