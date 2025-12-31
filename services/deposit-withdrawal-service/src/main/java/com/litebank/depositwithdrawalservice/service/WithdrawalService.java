package com.litebank.depositwithdrawalservice.service;

import com.litebank.depositwithdrawalservice.client.AccountServiceClient;
import com.litebank.depositwithdrawalservice.client.TransactionServiceClient;
import com.litebank.depositwithdrawalservice.dto.DebitRequest;
import com.litebank.depositwithdrawalservice.dto.TransactionResponse;
import com.litebank.depositwithdrawalservice.dto.WithdrawalRequest;
import com.litebank.depositwithdrawalservice.dto.WithdrawalResponse;
import com.litebank.depositwithdrawalservice.exception.DepositWithdrawalException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final Tracer tracer;

    public WithdrawalResponse executeWithdrawal(WithdrawalRequest request) {
        Span span = tracer.spanBuilder("WithdrawalService.executeWithdrawal")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        String withdrawalId = UUID.randomUUID().toString();
        String traceId = span.getSpanContext().getTraceId();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("withdrawal.id", withdrawalId);
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("amount", request.getAmount().toString());
            span.setAttribute("currency", request.getCurrency());

            log.info("Starting withdrawal: {} - {} {} from account {}",
                    withdrawalId, request.getAmount(), request.getCurrency(), request.getAccountId());

            // Step 1: Get account and validate currency
            Map<String, Object> account = accountServiceClient.getAccount(request.getAccountId());
            String accountCurrency = (String) account.get("currency");

            // Validate currency matches
            if (!accountCurrency.equalsIgnoreCase(request.getCurrency())) {
                throw new DepositWithdrawalException("ERR_WTH_002",
                        "Currency mismatch. Account currency: " + accountCurrency +
                                ", Withdrawal currency: " + request.getCurrency());
            }

            // Step 2: Execute atomic debit operation via Transaction Service
            // Note: Transaction Service will perform balance validation (insufficient funds check)
            log.info("Executing atomic debit operation via Transaction Service");

            String description = request.getDescription() != null ? request.getDescription()
                    : "Withdrawal from account";
            String referenceId = request.getReferenceId() != null ? request.getReferenceId() : withdrawalId;

            DebitRequest debitRequest = DebitRequest.builder()
                    .accountId(request.getAccountId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .transactionType("WITHDRAWAL")
                    .referenceId(referenceId)
                    .description(description)
                    .build();

            TransactionResponse txnResponse = transactionServiceClient.debit(debitRequest);

            log.info("Debit completed - Transaction: {}, Balance After: {}",
                    txnResponse.getTransactionId(),
                    txnResponse.getBalanceAfter());

            span.setStatus(StatusCode.OK);
            log.info("Withdrawal completed successfully: {}", withdrawalId);

            return WithdrawalResponse.builder()
                    .withdrawalId(withdrawalId)
                    .accountId(txnResponse.getAccountId())
                    .amount(txnResponse.getAmount())
                    .currency(txnResponse.getCurrency())
                    .balanceAfter(txnResponse.getBalanceAfter())
                    .status("COMPLETED")
                    .transactionId(txnResponse.getTransactionId())
                    .description(description)
                    .traceId(traceId)
                    .createdAt(txnResponse.getCreatedAt())
                    .build();

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Withdrawal failed: {} - {}", withdrawalId, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
