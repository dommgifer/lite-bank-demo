package com.litebank.exchangeservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorInfo error;
    private String traceId;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorInfo error, String traceId) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String code;
        private String type;
        private String message;
        private String category;
        private Object details;
        private String traceId;
        private String timestamp;
    }
}
