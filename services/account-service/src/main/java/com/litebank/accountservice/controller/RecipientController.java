package com.litebank.accountservice.controller;

import com.litebank.accountservice.dto.ApiResponse;
import com.litebank.accountservice.dto.CreateRecipientRequest;
import com.litebank.accountservice.dto.RecipientResponse;
import com.litebank.accountservice.service.RecipientService;
import io.opentelemetry.api.trace.Span;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipientResponse>>> getRecipientsByUser(
            @RequestParam Long userId) {
        Span.current().setAttribute("http.route", "/api/v1/recipients");
        Span.current().setAttribute("user.id", userId);

        List<RecipientResponse> recipients = recipientService.getRecipientsByUserId(userId);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(recipients, traceId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecipientResponse>> createRecipient(
            @Valid @RequestBody CreateRecipientRequest request) {
        Span.current().setAttribute("http.route", "/api/v1/recipients");
        Span.current().setAttribute("user.id", request.getUserId());
        Span.current().setAttribute("account.number", request.getAccountNumber());

        RecipientResponse response = recipientService.createRecipient(request);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, traceId));
    }

    @DeleteMapping("/{recipientId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipient(
            @PathVariable Long recipientId,
            @RequestParam Long userId) {
        Span.current().setAttribute("http.route", "/api/v1/recipients/{recipientId}");
        Span.current().setAttribute("recipient.id", recipientId);
        Span.current().setAttribute("user.id", userId);

        recipientService.deleteRecipient(recipientId, userId);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(null, traceId));
    }
}
