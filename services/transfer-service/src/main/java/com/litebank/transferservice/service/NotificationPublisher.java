package com.litebank.transferservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final String TOPIC = "banking.notifications";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final TextMapPropagator propagator;

    private static final TextMapSetter<Map<String, String>> SETTER = Map::put;

    public void publishTransferCompleted(String userId, String transferId,
                                         BigDecimal amount, String currency,
                                         String fromAccountNumber, String toAccountNumber) {
        Span span = tracer.spanBuilder("notification.publish.transfer_completed")
                .startSpan();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationId", UUID.randomUUID().toString());
            notification.put("userId", userId);
            notification.put("type", "TRANSFER_COMPLETED");
            notification.put("title", "轉帳成功");
            notification.put("message", String.format("已成功轉出 %s %s 至帳戶 %s",
                    amount.toPlainString(), currency, toAccountNumber));
            notification.put("timestamp", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("transferId", transferId);
            data.put("amount", amount);
            data.put("currency", currency);
            data.put("fromAccount", fromAccountNumber);
            data.put("toAccount", toAccountNumber);
            notification.put("data", data);

            // Add trace context
            Map<String, Object> trace = new HashMap<>();
            trace.put("traceId", Span.current().getSpanContext().getTraceId());
            trace.put("spanId", Span.current().getSpanContext().getSpanId());
            notification.put("trace", trace);

            publish(userId, notification);
            log.info("Published TRANSFER_COMPLETED notification for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to publish transfer completed notification", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    public void publishTransferFailed(String userId, String transferId,
                                      BigDecimal amount, String currency,
                                      String reason) {
        Span span = tracer.spanBuilder("notification.publish.transfer_failed")
                .startSpan();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationId", UUID.randomUUID().toString());
            notification.put("userId", userId);
            notification.put("type", "TRANSFER_FAILED");
            notification.put("title", "轉帳失敗");
            notification.put("message", String.format("轉帳 %s %s 失敗：%s",
                    amount.toPlainString(), currency, reason));
            notification.put("timestamp", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("transferId", transferId);
            data.put("amount", amount);
            data.put("currency", currency);
            data.put("reason", reason);
            notification.put("data", data);

            // Add trace context
            Map<String, Object> trace = new HashMap<>();
            trace.put("traceId", Span.current().getSpanContext().getTraceId());
            trace.put("spanId", Span.current().getSpanContext().getSpanId());
            notification.put("trace", trace);

            publish(userId, notification);
            log.info("Published TRANSFER_FAILED notification for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to publish transfer failed notification", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    private void publish(String userId, Map<String, Object> notification) throws JsonProcessingException {
        String message = objectMapper.writeValueAsString(notification);

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, userId, message);

        // Inject trace context into Kafka headers
        Map<String, String> headers = new HashMap<>();
        propagator.inject(Context.current(), headers, SETTER);
        headers.forEach((key, value) ->
                record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));

        kafkaTemplate.send(record);
    }
}
