package com.litebank.tellerservice.controller;

import com.litebank.tellerservice.dto.ApiResponse;
import com.litebank.tellerservice.dto.DepositRequest;
import com.litebank.tellerservice.dto.DepositResponse;
import com.litebank.tellerservice.service.DepositService;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
@Slf4j
public class DepositController {

    private final DepositService depositService;

    @PostMapping
    public ResponseEntity<ApiResponse<DepositResponse>> deposit(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Valid @RequestBody DepositRequest request) {

        Span.current().setAttribute("http.route", "/api/v1/deposits");
        Span.current().setAttribute("account.id", request.getAccountId());
        Span.current().setAttribute("amount", request.getAmount().toString());
        Span.current().setAttribute("currency", request.getCurrency());
        if (userId != null) {
            Span.current().setAttribute("user.id", userId);
        }

        DepositResponse response = depositService.executeDeposit(request, userId);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, traceId));
    }
}
