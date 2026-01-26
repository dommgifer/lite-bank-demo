package com.litebank.transactionservice.service;

import com.litebank.transactionservice.entity.Transaction;
import com.litebank.transactionservice.event.TransactionCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic.transaction-events:transaction-events}")
    private String transactionEventsTopic;

    /**
     * Publishes a transaction created event to Kafka
     * @param transaction The transaction entity
     * @param userId The user ID who owns the account
     */
    public void publishTransactionCreated(Transaction transaction, Long userId) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TRANSACTION_CREATED")
                .timestamp(LocalDateTime.now())
                .payload(TransactionCreatedEvent.TransactionPayload.builder()
                        .transactionId(transaction.getTransactionId())
                        .userId(userId)
                        .accountId(transaction.getAccountId())
                        .amount(transaction.getAmount())
                        .transactionType(transaction.getTransactionType().name())
                        .currency(transaction.getCurrency())
                        .balanceAfter(transaction.getBalanceAfter())
                        .description(transaction.getDescription())
                        .traceId(transaction.getTraceId())
                        .createdAt(transaction.getCreatedAt())
                        .build())
                .build();

        String key = String.valueOf(userId);  // Use userId as partition key for ordering

        CompletableFuture<SendResult<String, TransactionCreatedEvent>> future =
                kafkaTemplate.send(transactionEventsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction event published successfully: eventId={}, transactionId={}, userId={}, topic={}, partition={}, offset={}",
                        event.getEventId(),
                        transaction.getTransactionId(),
                        userId,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish transaction event: eventId={}, transactionId={}, userId={}, error={}",
                        event.getEventId(),
                        transaction.getTransactionId(),
                        userId,
                        ex.getMessage(),
                        ex);
            }
        });
    }
}
