package com.litebank.accountservice.controller;

import com.litebank.accountservice.dto.AccountPublicInfoResponse;
import com.litebank.accountservice.dto.AccountResponse;
import com.litebank.accountservice.dto.ApiResponse;
import com.litebank.accountservice.dto.BalanceResponse;
import com.litebank.accountservice.dto.CreateAccountRequest;
import com.litebank.accountservice.entity.Account;
import com.litebank.accountservice.service.AccountService;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long accountId) {
        Span.current().setAttribute("http.route", "/api/v1/accounts/{accountId}");
        Span.current().setAttribute("account.id", accountId);

        Account account = accountService.getAccountById(accountId);
        AccountResponse response = AccountResponse.fromEntity(account);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(response, traceId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByUser(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(required = false) Long userId) {
        Long effectiveUserId = headerUserId != null ? headerUserId : userId;
        if (effectiveUserId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        Span.current().setAttribute("http.route", "/api/v1/accounts");
        Span.current().setAttribute("user.id", effectiveUserId);

        List<Account> accounts = accountService.getAccountsByUserId(effectiveUserId);
        List<AccountResponse> response = accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(response, traceId));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountPublicInfoResponse>> getAccountByNumber(@PathVariable String accountNumber) {
        Span.current().setAttribute("http.route", "/api/v1/accounts/number/{accountNumber}");
        Span.current().setAttribute("account.number", accountNumber);

        AccountPublicInfoResponse response = accountService.getPublicAccountInfo(accountNumber);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(response, traceId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long accountId) {
        Span.current().setAttribute("http.route", "/api/v1/accounts/{accountId}/balance");
        Span.current().setAttribute("account.id", accountId);

        BalanceResponse response = accountService.getAccountBalance(accountId);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(response, traceId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        Span.current().setAttribute("http.route", "/api/v1/accounts");
        Span.current().setAttribute("user.id", request.getUserId());
        Span.current().setAttribute("account.currency", request.getCurrency());

        Account account = accountService.createAccount(request);
        AccountResponse response = AccountResponse.fromEntity(account);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, traceId));
    }

    // PUT /{accountId}/balance endpoint removed - balance updates are now exclusively handled by Transaction Service
}
