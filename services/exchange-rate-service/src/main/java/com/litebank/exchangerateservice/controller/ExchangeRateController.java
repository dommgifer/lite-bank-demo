package com.litebank.exchangerateservice.controller;

import com.litebank.exchangerateservice.dto.ApiResponse;
import com.litebank.exchangerateservice.dto.ExchangeRateResponse;
import com.litebank.exchangerateservice.service.ExchangeRateService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/rates")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final Tracer tracer;

    /**
     * Get exchange rate between two currencies
     * GET /api/v1/rates/{from}/{to}
     */
    @GetMapping("/{from}/{to}")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getExchangeRate(
            @PathVariable String from,
            @PathVariable String to) {

        Span span = tracer.spanBuilder("GET /api/v1/rates/{from}/{to}")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/rates/{from}/{to}");
            span.setAttribute("from.currency", from);
            span.setAttribute("to.currency", to);

            ExchangeRateResponse response = exchangeRateService.getExchangeRate(from, to);
            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    /**
     * Get all supported currencies
     * GET /api/v1/rates/currencies
     */
    @GetMapping("/currencies")
    public ResponseEntity<ApiResponse<Set<String>>> getSupportedCurrencies() {
        Span span = tracer.spanBuilder("GET /api/v1/rates/currencies")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/rates/currencies");

            Set<String> currencies = exchangeRateService.getSupportedCurrencies();
            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(currencies, traceId));

        } finally {
            span.end();
        }
    }

    /**
     * Convert amount from one currency to another
     * GET /api/v1/rates/convert?from=USD&to=TWD&amount=100
     */
    @GetMapping("/convert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> convertAmount(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {

        Span span = tracer.spanBuilder("GET /api/v1/rates/convert")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/rates/convert");
            span.setAttribute("from.currency", from);
            span.setAttribute("to.currency", to);
            span.setAttribute("amount", amount.toString());

            ExchangeRateResponse rateResponse = exchangeRateService.getExchangeRate(from, to);
            BigDecimal convertedAmount = exchangeRateService.convertAmount(from, to, amount);

            Map<String, Object> result = Map.of(
                    "fromCurrency", from.toUpperCase(),
                    "toCurrency", to.toUpperCase(),
                    "originalAmount", amount,
                    "convertedAmount", convertedAmount,
                    "rate", rateResponse.getRate(),
                    "timestamp", Instant.now().toString()
            );

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(result, traceId));

        } finally {
            span.end();
        }
    }

}
