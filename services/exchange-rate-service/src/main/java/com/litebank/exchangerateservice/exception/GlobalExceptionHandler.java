package com.litebank.exchangerateservice.exception;

import com.litebank.exchangerateservice.dto.ApiResponse;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CurrencyNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCurrencyNotSupported(CurrencyNotSupportedException e) {
        String traceId = getCurrentTraceId();
        log.warn("Currency not supported: {}, trace_id: {}", e.getMessage(), traceId);

        ApiResponse.ErrorInfo error = ApiResponse.ErrorInfo.builder()
                .code("ERR_RATE_001")
                .type("CURRENCY_NOT_SUPPORTED")
                .message(e.getMessage())
                .category("validation")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("X-Trace-Id", traceId)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        String traceId = getCurrentTraceId();
        log.error("Unexpected error occurred, trace_id: {}", traceId, e);

        ApiResponse.ErrorInfo error = ApiResponse.ErrorInfo.builder()
                .code("ERR_SYS_001")
                .type("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .category("system")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Trace-Id", traceId)
                .body(ApiResponse.error(error, traceId));
    }

    private String getCurrentTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return "00000000000000000000000000000000";
    }
}
