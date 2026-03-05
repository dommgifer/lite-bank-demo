package com.litebank.transferservice.service;

import com.litebank.transferservice.dto.*;
import com.litebank.transferservice.exception.InsufficientBalanceException;
import com.litebank.transferservice.exception.InvalidTransferException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final NotificationPublisher notificationPublisher;
    private final Tracer tracer;

    public TransferResponse transfer(TransferRequest request, String traceId, String userId) {
        Span span = tracer.spanBuilder("TransferService.transfer")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("from.account.id", request.getFromAccountId());
            span.setAttribute("to.account.id", request.getToAccountId());
            span.setAttribute("amount", request.getAmount().toString());
            span.setAttribute("currency", request.getCurrency());

            // Validate transfer
            validateTransfer(request);

            // Generate transfer ID
            String transferId = UUID.randomUUID().toString();
            span.setAttribute("transfer.id", transferId);

            // Step 1: Get source account
            log.info("Getting source account: {}", request.getFromAccountId());
            AccountResponse fromAccount = accountServiceClient.getAccount(request.getFromAccountId());

            // Step 2: Get destination account
            log.info("Getting destination account: {}", request.getToAccountId());
            AccountResponse toAccount = accountServiceClient.getAccount(request.getToAccountId());

            // Validate currency match
            if (!fromAccount.getCurrency().equals(request.getCurrency())) {
                throw new InvalidTransferException("Source account currency does not match transfer currency");
            }
            if (!toAccount.getCurrency().equals(request.getCurrency())) {
                throw new InvalidTransferException("Destination account currency does not match transfer currency");
            }

            // Validate sufficient balance
            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in source account");
            }

            // Validate account status
            if (!"ACTIVE".equals(fromAccount.getStatus())) {
                throw new InvalidTransferException("Source account is not active");
            }
            if (!"ACTIVE".equals(toAccount.getStatus())) {
                throw new InvalidTransferException("Destination account is not active");
            }

            // Step 3: Execute atomic transfer via Transaction Service
            log.info("Executing atomic transfer via Transaction Service");

            String description = "Transfer from account " + fromAccount.getAccountNumber() +
                    " to account " + toAccount.getAccountNumber() +
                    (request.getDescription() != null ? ": " + request.getDescription() : "");

            TransferTransactionRequest transferRequest = TransferTransactionRequest.builder()
                    .sourceAccountId(request.getFromAccountId())
                    .destinationAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .referenceId(transferId)
                    .description(description)
                    .build();

            TransferTransactionResponse transferResult = transactionServiceClient.transfer(transferRequest);

            log.info("Transfer completed - Source Txn: {}, Dest Txn: {}, Source Balance: {}, Dest Balance: {}",
                    transferResult.getSourceTransactionId(),
                    transferResult.getDestinationTransactionId(),
                    transferResult.getSourceBalanceAfter(),
                    transferResult.getDestinationBalanceAfter());

            // Build response
            TransferResponse response = TransferResponse.builder()
                    .transferId(transferId)
                    .fromAccountId(transferResult.getSourceAccountId())
                    .toAccountId(transferResult.getDestinationAccountId())
                    .amount(transferResult.getAmount())
                    .currency(transferResult.getCurrency())
                    .description(request.getDescription())
                    .status("COMPLETED")
                    .fromTransactionId(transferResult.getSourceTransactionId())
                    .toTransactionId(transferResult.getDestinationTransactionId())
                    .traceId(traceId)
                    .createdAt(transferResult.getCreatedAt())
                    .build();

            log.info("Transfer completed successfully: {}", transferId);

            // Publish notification to sender (async, non-blocking)
            if (userId != null) {
                try {
                    notificationPublisher.publishTransferCompleted(
                            userId,
                            transferId,
                            request.getAmount(),
                            request.getCurrency(),
                            fromAccount.getAccountNumber(),
                            toAccount.getAccountNumber()
                    );
                } catch (Exception e) {
                    // Notification failure should not affect transfer result
                    log.warn("Failed to publish notification for transfer {}: {}", transferId, e.getMessage());
                }
            }

            // Publish notification to receiver (async, non-blocking)
            if (toAccount.getUserId() != null) {
                try {
                    notificationPublisher.publishTransferReceived(
                            String.valueOf(toAccount.getUserId()),
                            transferId,
                            request.getAmount(),
                            request.getCurrency(),
                            fromAccount.getAccountNumber(),
                            toAccount.getAccountNumber()
                    );
                } catch (Exception e) {
                    // Notification failure should not affect transfer result
                    log.warn("Failed to publish notification to receiver for transfer {}: {}", transferId, e.getMessage());
                }
            }

            return response;

        } catch (InsufficientBalanceException | InvalidTransferException e) {
            span.recordException(e);
            log.error("Transfer validation failed: {}", e.getMessage());
            // Publish failure notification
            if (userId != null) {
                try {
                    notificationPublisher.publishTransferFailed(
                            userId,
                            UUID.randomUUID().toString(),
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
            span.recordException(e);
            log.error("Transfer failed: {}", e.getMessage(), e);
            throw new RuntimeException("Transfer failed: " + e.getMessage(), e);
        } finally {
            span.end();
        }
    }

    private void validateTransfer(TransferRequest request) {
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new InvalidTransferException("Cannot transfer to the same account");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be greater than zero");
        }
    }
}
