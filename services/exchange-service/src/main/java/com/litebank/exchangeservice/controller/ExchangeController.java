package com.litebank.exchangeservice.controller;

import com.litebank.exchangeservice.dto.ApiResponse;
import com.litebank.exchangeservice.dto.ExchangeRequest;
import com.litebank.exchangeservice.dto.ExchangeResponse;
import com.litebank.exchangeservice.service.ExchangeService;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/exchanges")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final ExchangeService exchangeService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExchangeResponse>> executeExchange(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Valid @RequestBody ExchangeRequest request) {

        Span.current().setAttribute("http.route", "/api/v1/exchanges");
        Span.current().setAttribute("source.account.id", request.getSourceAccountId());
        Span.current().setAttribute("destination.account.id", request.getDestinationAccountId());
        Span.current().setAttribute("amount", request.getAmount().toString());
        if (userId != null) {
            Span.current().setAttribute("user.id", userId);
        }

        ExchangeResponse response = exchangeService.executeExchange(request, userId);
        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(response, traceId));
    }

}
