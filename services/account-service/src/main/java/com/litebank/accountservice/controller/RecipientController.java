package com.litebank.accountservice.controller;

import com.litebank.accountservice.dto.ApiResponse;
import com.litebank.accountservice.dto.CreateRecipientRequest;
import com.litebank.accountservice.dto.RecipientResponse;
import com.litebank.accountservice.service.RecipientService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipients")
@RequiredArgsConstructor
@Slf4j
public class RecipientController {

    private final RecipientService recipientService;
    private final Tracer tracer;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipientResponse>>> getRecipientsByUser(
            @RequestParam Long userId) {
        Span span = tracer.spanBuilder("GET /api/v1/recipients")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/recipients");
            span.setAttribute("user.id", userId);

            List<RecipientResponse> recipients = recipientService.getRecipientsByUserId(userId);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(recipients, traceId));

        } finally {
            span.end();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecipientResponse>> createRecipient(
            @Valid @RequestBody CreateRecipientRequest request) {
        Span span = tracer.spanBuilder("POST /api/v1/recipients")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/api/v1/recipients");
            span.setAttribute("user.id", request.getUserId());
            span.setAttribute("account.number", request.getAccountNumber());

            RecipientResponse response = recipientService.createRecipient(request);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    @DeleteMapping("/{recipientId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipient(
            @PathVariable Long recipientId,
            @RequestParam Long userId) {
        Span span = tracer.spanBuilder("DELETE /api/v1/recipients/{recipientId}")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "DELETE");
            span.setAttribute("http.route", "/api/v1/recipients/{recipientId}");
            span.setAttribute("recipient.id", recipientId);
            span.setAttribute("user.id", userId);

            recipientService.deleteRecipient(recipientId, userId);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(null, traceId));

        } finally {
            span.end();
        }
    }
}
