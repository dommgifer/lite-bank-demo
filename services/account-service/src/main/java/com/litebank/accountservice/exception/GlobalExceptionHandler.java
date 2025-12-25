package com.litebank.accountservice.exception;

import com.litebank.accountservice.dto.ApiResponse;
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

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFoundException(AccountNotFoundException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        log.error("Account not found: {}, trace_id: {}", ex.getMessage(), traceId);

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_ACC_001")
                .type("ACCOUNT_NOT_FOUND")
                .message(ex.getMessage())
                .category("account")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation failed: {}, trace_id: {}", errors, traceId);

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
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
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        log.error("Unexpected error occurred, trace_id: {}", traceId, ex);

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_SYS_001")
                .type("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .category("system")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error, traceId));
    }
}
