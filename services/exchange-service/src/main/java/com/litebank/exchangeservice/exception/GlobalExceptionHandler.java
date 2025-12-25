package com.litebank.exchangeservice.exception;

import com.litebank.exchangeservice.dto.ApiResponse;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException e) {
        String traceId = getCurrentTraceId();

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}, trace_id: {}", errors, traceId);

        ApiResponse.ErrorInfo error = ApiResponse.ErrorInfo.builder()
                .code("ERR_VAL_001")
                .type("VALIDATION_ERROR")
                .message("Request validation failed")
                .category("validation")
                .details(errors)
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("X-Trace-Id", traceId)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(ExchangeException.class)
    public ResponseEntity<ApiResponse<Void>> handleExchangeException(ExchangeException e) {
        String traceId = getCurrentTraceId();
        log.warn("Exchange error: {} - {}, trace_id: {}", e.getCode(), e.getMessage(), traceId);

        ApiResponse.ErrorInfo error = ApiResponse.ErrorInfo.builder()
                .code(e.getCode())
                .type("EXCHANGE_ERROR")
                .message(e.getMessage())
                .category("business")
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
