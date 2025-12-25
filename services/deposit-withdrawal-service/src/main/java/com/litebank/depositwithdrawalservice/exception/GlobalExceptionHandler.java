package com.litebank.depositwithdrawalservice.exception;

import com.litebank.depositwithdrawalservice.dto.ApiResponse;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DepositWithdrawalException.class)
    public ResponseEntity<ApiResponse<Void>> handleDepositWithdrawalException(DepositWithdrawalException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.error("DepositWithdrawalException: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = determineHttpStatus(ex.getErrorCode());

        return ResponseEntity.status(status)
                .header("X-Trace-Id", traceId)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.error("Validation error: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header("X-Trace-Id", traceId)
                .body(ApiResponse.error("ERR_VALIDATION", message, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        String traceId = Span.current().getSpanContext().getTraceId();
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Trace-Id", traceId)
                .body(ApiResponse.error("ERR_INTERNAL", "Internal server error", traceId));
    }

    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) return HttpStatus.INTERNAL_SERVER_ERROR;

        return switch (errorCode) {
            case "ERR_DEP_001", "ERR_WTH_001" -> HttpStatus.NOT_FOUND;
            case "ERR_DEP_002", "ERR_WTH_002" -> HttpStatus.BAD_REQUEST;
            case "ERR_WTH_003" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
