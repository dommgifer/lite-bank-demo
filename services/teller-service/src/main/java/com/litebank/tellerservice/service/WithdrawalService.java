package com.litebank.tellerservice.service;

import com.litebank.tellerservice.dto.*;
import com.litebank.tellerservice.exception.TellerException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final NotificationPublisher notificationPublisher;
    private final Tracer tracer;

    public WithdrawalResponse executeWithdrawal(WithdrawalRequest request, String userId) {
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
            AccountResponse account = accountServiceClient.getAccount(request.getAccountId());
            String accountCurrency = account.getCurrency();
            String accountNumber = account.getAccountNumber();

            if (!accountCurrency.equalsIgnoreCase(request.getCurrency())) {
                throw new TellerException("ERR_WTH_002",
                        "Currency mismatch. Account currency: " + accountCurrency +
                                ", Withdrawal currency: " + request.getCurrency());
            }

            // Step 2: Execute atomic debit operation via Transaction Service
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

            if (userId != null) {
                try {
                    notificationPublisher.publishWithdrawalCompleted(
                            userId,
                            withdrawalId,
                            request.getAmount(),
                            request.getCurrency(),
                            accountNumber,
                            txnResponse.getBalanceAfter()
                    );
                } catch (Exception e) {
                    log.warn("Failed to publish notification for withdrawal {}: {}", withdrawalId, e.getMessage());
                }
            }

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

        } catch (TellerException e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Withdrawal failed: {} - {}", withdrawalId, e.getMessage());
            if (userId != null) {
                try {
                    notificationPublisher.publishWithdrawalFailed(
                            userId,
                            withdrawalId,
                            request.getAmount(),
                            request.getCurrency(),
                            e.getMessage()
                    );
                } catch (Exception ex) {
                    log.warn("Failed to publish failure notification: {}", ex.getMessage());
                }
            }
            throw e;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Withdrawal failed: {} - {}", withdrawalId, e.getMessage());
            if (userId != null) {
                try {
                    notificationPublisher.publishWithdrawalFailed(
                            userId,
                            withdrawalId,
                            request.getAmount(),
                            request.getCurrency(),
                            e.getMessage()
                    );
                } catch (Exception ex) {
                    log.warn("Failed to publish failure notification: {}", ex.getMessage());
                }
            }
            throw new RuntimeException("Withdrawal failed: " + e.getMessage(), e);
        } finally {
            span.end();
        }
    }
}
