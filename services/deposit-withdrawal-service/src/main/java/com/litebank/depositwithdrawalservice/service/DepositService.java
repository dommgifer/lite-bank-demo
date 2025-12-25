package com.litebank.depositwithdrawalservice.service;

import com.litebank.depositwithdrawalservice.client.AccountServiceClient;
import com.litebank.depositwithdrawalservice.client.TransactionServiceClient;
import com.litebank.depositwithdrawalservice.dto.CreditRequest;
import com.litebank.depositwithdrawalservice.dto.DepositRequest;
import com.litebank.depositwithdrawalservice.dto.DepositResponse;
import com.litebank.depositwithdrawalservice.dto.TransactionResponse;
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
public class DepositService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final Tracer tracer;

    public DepositResponse executeDeposit(DepositRequest request) {
        Span span = tracer.spanBuilder("DepositService.executeDeposit")
                .startSpan();

        String depositId = UUID.randomUUID().toString();
        String traceId = span.getSpanContext().getTraceId();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("deposit.id", depositId);
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("amount", request.getAmount().toString());
            span.setAttribute("currency", request.getCurrency());

            log.info("Starting deposit: {} - {} {} to account {}",
                    depositId, request.getAmount(), request.getCurrency(), request.getAccountId());

            // Step 1: Get account and validate currency
            Map<String, Object> account = accountServiceClient.getAccount(request.getAccountId());
            String accountCurrency = (String) account.get("currency");

            // Validate currency matches
            if (!accountCurrency.equalsIgnoreCase(request.getCurrency())) {
                throw new DepositWithdrawalException("ERR_DEP_002",
                        "Currency mismatch. Account currency: " + accountCurrency +
                                ", Deposit currency: " + request.getCurrency());
            }

            // Step 2: Execute atomic credit operation via Transaction Service
            log.info("Executing atomic credit operation via Transaction Service");

            String description = request.getDescription() != null ? request.getDescription()
                    : "Deposit to account";
            String referenceId = request.getReferenceId() != null ? request.getReferenceId() : depositId;

            CreditRequest creditRequest = CreditRequest.builder()
                    .accountId(request.getAccountId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .referenceId(referenceId)
                    .description(description)
                    .build();

            TransactionResponse txnResponse = transactionServiceClient.credit(creditRequest);

            log.info("Credit completed - Transaction: {}, Balance After: {}",
                    txnResponse.getTransactionId(),
                    txnResponse.getBalanceAfter());

            span.setStatus(StatusCode.OK);
            log.info("Deposit completed successfully: {}", depositId);

            return DepositResponse.builder()
                    .depositId(depositId)
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
            log.error("Deposit failed: {} - {}", depositId, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
