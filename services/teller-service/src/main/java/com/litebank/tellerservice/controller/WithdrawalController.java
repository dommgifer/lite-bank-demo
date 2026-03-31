package com.litebank.tellerservice.controller;

import com.litebank.tellerservice.dto.ApiResponse;
import com.litebank.tellerservice.dto.WithdrawalRequest;
import com.litebank.tellerservice.dto.WithdrawalResponse;
import com.litebank.tellerservice.service.WithdrawalService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
@Slf4j
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final Tracer tracer;

    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalResponse>> withdraw(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Valid @RequestBody WithdrawalRequest request) {
        Span span = tracer.spanBuilder("POST /api/v1/withdrawals")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/api/v1/withdrawals");
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("amount", request.getAmount().toString());
            span.setAttribute("currency", request.getCurrency());
            if (userId != null) {
                span.setAttribute("user.id", userId);
            }

            WithdrawalResponse response = withdrawalService.executeWithdrawal(request, userId);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }
}
