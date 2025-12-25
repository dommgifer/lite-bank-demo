package com.litebank.transferservice.exception;

import com.litebank.transferservice.dto.ApiResponse;
import io.opentelemetry.api.trace.Span;
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
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_TRF_001")
                .type("INSUFFICIENT_BALANCE")
                .message(ex.getMessage())
                .category("transfer")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransferException(InvalidTransferException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_TRF_002")
                .type("INVALID_TRANSFER")
                .message(ex.getMessage())
                .category("transfer")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(ServiceCommunicationException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceCommunicationException(ServiceCommunicationException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_TRF_003")
                .type("SERVICE_COMMUNICATION_ERROR")
                .message(ex.getMessage())
                .category("transfer")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

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
