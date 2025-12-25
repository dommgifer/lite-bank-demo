package com.litebank.transactionservice.exception;

import com.litebank.transactionservice.dto.ApiResponse;
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

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionNotFoundException(TransactionNotFoundException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_TXN_001")
                .type("TRANSACTION_NOT_FOUND")
                .message(ex.getMessage())
                .category("transaction")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFoundException(AccountNotFoundException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

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

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_ACC_002")
                .type("INSUFFICIENT_BALANCE")
                .message(ex.getMessage())
                .category("account")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(error, traceId));
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleCurrencyMismatchException(CurrencyMismatchException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_ACC_003")
                .type("CURRENCY_MISMATCH")
                .message(ex.getMessage())
                .category("account")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String traceId = Span.current().getSpanContext().getTraceId();

        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ERR_VAL_002")
                .type("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .category("validation")
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
