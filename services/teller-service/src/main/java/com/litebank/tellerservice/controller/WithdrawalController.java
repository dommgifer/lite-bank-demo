package com.litebank.tellerservice.controller;

import com.litebank.tellerservice.dto.ApiResponse;
import com.litebank.tellerservice.dto.WithdrawalRequest;
import com.litebank.tellerservice.dto.WithdrawalResponse;
import com.litebank.tellerservice.service.WithdrawalService;
import io.opentelemetry.api.trace.Span;
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

    @PostMapping
    public ResponseEntity<ApiResponse<WithdrawalResponse>> withdraw(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Valid @RequestBody WithdrawalRequest request) {

        Span.current().setAttribute("http.route", "/api/v1/withdrawals");
        Span.current().setAttribute("account.id", request.getAccountId());
        Span.current().setAttribute("amount", request.getAmount().toString());
        Span.current().setAttribute("currency", request.getCurrency());
        if (userId != null) {
            Span.current().setAttribute("user.id", userId);
        }

        WithdrawalResponse response = withdrawalService.executeWithdrawal(request, userId);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, traceId));
    }
}
