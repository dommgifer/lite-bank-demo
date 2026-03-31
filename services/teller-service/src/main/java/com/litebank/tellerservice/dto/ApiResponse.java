package com.litebank.tellerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorDetails error;
    private String traceId;

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .traceId(traceId)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorDetails error, String traceId) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .traceId(traceId)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        private String code;
        private String type;
        private String message;
        private String category;
        private Object details;
        private String traceId;
        private String timestamp;
    }
}
