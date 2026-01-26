package com.litebank.analytics.processor.consumer;

import com.litebank.analytics.processor.model.event.TransactionCreatedEvent;
import com.litebank.analytics.processor.service.BalanceSnapshotService;
import com.litebank.analytics.processor.service.DailySummaryService;
import com.litebank.analytics.processor.service.MonthlySummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final DailySummaryService dailySummaryService;
    private final MonthlySummaryService monthlySummaryService;
    private final BalanceSnapshotService balanceSnapshotService;

    @KafkaListener(
            topics = "${kafka.topic.transaction-events:transaction-events}",
            groupId = "${spring.kafka.consumer.group-id:analytics-processor}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(
            @Payload TransactionCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received transaction event: eventId={}, transactionId={}, userId={}, partition={}, offset={}",
                event.getEventId(),
                event.getPayload().getTransactionId(),
                event.getPayload().getUserId(),
                partition,
                offset);

        try {
            // Update daily summary
            dailySummaryService.updateDailySummary(event);

            // Update monthly summary
            monthlySummaryService.updateMonthlySummary(event);

            // Update balance snapshot
            balanceSnapshotService.updateBalanceSnapshot(event);

            log.info("Successfully processed transaction event: eventId={}, transactionId={}",
                    event.getEventId(),
                    event.getPayload().getTransactionId());

        } catch (Exception e) {
            log.error("Failed to process transaction event: eventId={}, transactionId={}, error={}",
                    event.getEventId(),
                    event.getPayload().getTransactionId(),
                    e.getMessage(),
                    e);
            // Let the exception propagate for Kafka error handling (retry/dead letter queue)
            throw e;
        }
    }
}
