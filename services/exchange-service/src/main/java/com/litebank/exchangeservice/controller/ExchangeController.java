package com.litebank.exchangeservice.controller;

import com.litebank.exchangeservice.dto.ApiResponse;
import com.litebank.exchangeservice.dto.ExchangeRequest;
import com.litebank.exchangeservice.dto.ExchangeResponse;
import com.litebank.exchangeservice.service.ExchangeService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exchanges")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final Tracer tracer;

    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeResponse>> executeExchange(
            @Valid @RequestBody ExchangeRequest request) {

        Span span = tracer.spanBuilder("POST /api/v1/exchanges")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/api/v1/exchanges");
            span.setAttribute("source.account.id", request.getSourceAccountId());
            span.setAttribute("destination.account.id", request.getDestinationAccountId());
            span.setAttribute("amount", request.getAmount().toString());

            ExchangeResponse response = exchangeService.executeExchange(request);
            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

}
