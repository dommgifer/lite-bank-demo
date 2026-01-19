package com.litebank.accountservice.service;

import com.litebank.accountservice.dto.AccountPublicInfoResponse;
import com.litebank.accountservice.dto.CreateRecipientRequest;
import com.litebank.accountservice.dto.RecipientResponse;
import com.litebank.accountservice.entity.Recipient;
import com.litebank.accountservice.exception.AccountNotFoundException;
import com.litebank.accountservice.repository.RecipientRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final AccountService accountService;
    private final Tracer tracer;

    @Transactional(readOnly = true)
    public List<RecipientResponse> getRecipientsByUserId(Long userId) {
        Span span = tracer.spanBuilder("RecipientService.getRecipientsByUserId")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("user.id", userId);

            log.debug("Fetching recipients for user ID: {}", userId);

            List<Recipient> recipients = recipientRepository.findByUserId(userId);

            // Enrich recipients with account public info
            List<RecipientResponse> responses = recipients.stream()
                    .map(recipient -> {
                        try {
                            AccountPublicInfoResponse accountInfo =
                                    accountService.getPublicAccountInfo(recipient.getAccountNumber());
                            return RecipientResponse.fromEntityWithAccountInfo(recipient, accountInfo);
                        } catch (AccountNotFoundException e) {
                            // If account not found, return recipient info without account details
                            log.warn("Account {} not found for recipient {}",
                                    recipient.getAccountNumber(), recipient.getRecipientId());
                            return RecipientResponse.fromEntity(recipient);
                        }
                    })
                    .collect(Collectors.toList());

            span.setAttribute("recipients.count", responses.size());
            span.setStatus(StatusCode.OK);

            return responses;
        } finally {
            span.end();
        }
    }

    @Transactional
    public RecipientResponse createRecipient(CreateRecipientRequest request) {
        Span span = tracer.spanBuilder("RecipientService.createRecipient")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("user.id", request.getUserId());
            span.setAttribute("account.number", request.getAccountNumber());

            log.info("Creating recipient for user {}: account {}",
                    request.getUserId(), request.getAccountNumber());

            // Validate that the account exists
            AccountPublicInfoResponse accountInfo;
            try {
                accountInfo = accountService.getPublicAccountInfo(request.getAccountNumber());
            } catch (AccountNotFoundException e) {
                span.setStatus(StatusCode.ERROR, "Account not found");
                throw e;
            }

            // Create recipient
            Recipient recipient = Recipient.builder()
                    .userId(request.getUserId())
                    .accountNumber(request.getAccountNumber())
                    .nickname(request.getNickname())
                    .build();

            try {
                Recipient savedRecipient = recipientRepository.save(recipient);
                span.setAttribute("recipient.id", savedRecipient.getRecipientId());
                span.setStatus(StatusCode.OK);

                log.info("Recipient created successfully: ID={}", savedRecipient.getRecipientId());

                return RecipientResponse.fromEntityWithAccountInfo(savedRecipient, accountInfo);
            } catch (DataIntegrityViolationException e) {
                span.setStatus(StatusCode.ERROR, "Duplicate recipient");
                throw new IllegalArgumentException(
                        "Recipient already exists for account: " + request.getAccountNumber());
            }
        } finally {
            span.end();
        }
    }

    @Transactional
    public void deleteRecipient(Long recipientId, Long userId) {
        Span span = tracer.spanBuilder("RecipientService.deleteRecipient")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("recipient.id", recipientId);
            span.setAttribute("user.id", userId);

            log.info("Deleting recipient {}", recipientId);

            // Verify the recipient belongs to the user
            Recipient recipient = recipientRepository.findById(recipientId)
                    .orElseThrow(() -> {
                        span.setStatus(StatusCode.ERROR, "Recipient not found");
                        return new IllegalArgumentException("Recipient not found: " + recipientId);
                    });

            if (!recipient.getUserId().equals(userId)) {
                span.setStatus(StatusCode.ERROR, "Unauthorized");
                throw new IllegalArgumentException(
                        "Recipient " + recipientId + " does not belong to user " + userId);
            }

            recipientRepository.delete(recipient);
            span.setStatus(StatusCode.OK);

            log.info("Recipient {} deleted successfully", recipientId);
        } finally {
            span.end();
        }
    }
}
