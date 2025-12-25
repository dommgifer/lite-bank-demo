package com.litebank.depositwithdrawalservice.controller;

import com.litebank.depositwithdrawalservice.dto.ApiResponse;
import com.litebank.depositwithdrawalservice.dto.WithdrawalRequest;
import com.litebank.depositwithdrawalservice.dto.WithdrawalResponse;
import com.litebank.depositwithdrawalservice.service.WithdrawalService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
@Slf4j
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final Tracer tracer;

    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalResponse>> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        Span span = tracer.spanBuilder("POST /api/v1/withdrawals")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/api/v1/withdrawals");
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("amount", request.getAmount().toString());
            span.setAttribute("currency", request.getCurrency());

            WithdrawalResponse response = withdrawalService.executeWithdrawal(request);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

}
