package com.litebank.accountservice.controller;

import com.litebank.accountservice.dto.AccountPublicInfoResponse;
import com.litebank.accountservice.dto.AccountResponse;
import com.litebank.accountservice.dto.ApiResponse;
import com.litebank.accountservice.dto.BalanceResponse;
import com.litebank.accountservice.dto.CreateAccountRequest;
import com.litebank.accountservice.entity.Account;
import com.litebank.accountservice.service.AccountService;
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
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final Tracer tracer;

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long accountId) {
        Span span = tracer.spanBuilder("GET /api/v1/accounts/{accountId}")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/accounts/{accountId}");
            span.setAttribute("account.id", accountId);

            Account account = accountService.getAccountById(accountId);
            AccountResponse response = AccountResponse.fromEntity(account);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByUser(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Long userId) {
        Long effectiveUserId = headerUserId != null ? headerUserId : userId;
        if (effectiveUserId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        Span span = tracer.spanBuilder("GET /api/v1/accounts")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/accounts");
            span.setAttribute("user.id", effectiveUserId);

            List<Account> accounts = accountService.getAccountsByUserId(effectiveUserId);
            List<AccountResponse> response = accounts.stream()
                    .map(AccountResponse::fromEntity)
                    .collect(Collectors.toList());

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountPublicInfoResponse>> getAccountByNumber(@PathVariable String accountNumber) {
        Span span = tracer.spanBuilder("GET /api/v1/accounts/number/{accountNumber}")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/accounts/number/{accountNumber}");
            span.setAttribute("account.number", accountNumber);

            AccountPublicInfoResponse response = accountService.getPublicAccountInfo(accountNumber);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long accountId) {
        Span span = tracer.spanBuilder("GET /api/v1/accounts/{accountId}/balance")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "GET");
            span.setAttribute("http.route", "/api/v1/accounts/{accountId}/balance");
            span.setAttribute("account.id", accountId);

            BalanceResponse response = accountService.getAccountBalance(accountId);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        Span span = tracer.spanBuilder("POST /api/v1/accounts")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/api/v1/accounts");
            span.setAttribute("user.id", request.getUserId());
            span.setAttribute("account.currency", request.getCurrency());

            Account account = accountService.createAccount(request);
            AccountResponse response = AccountResponse.fromEntity(account);

            String traceId = span.getSpanContext().getTraceId();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

    // PUT /{accountId}/balance endpoint removed - balance updates are now exclusively handled by Transaction Service
}
