package com.litebank.depositwithdrawalservice.service;

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

    public void publishDepositCompleted(String userId, String depositId,
                                        BigDecimal amount, String currency,
                                        String accountNumber, BigDecimal balanceAfter) {
        Span span = tracer.spanBuilder("notification.publish.deposit_completed")
                .startSpan();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationId", UUID.randomUUID().toString());
            notification.put("userId", userId);
            notification.put("type", "DEPOSIT_SUCCESS");
            notification.put("title", "存款成功");
            notification.put("message", String.format("已成功存入 %s %s 至帳戶 %s，餘額: %s",
                    amount.toPlainString(), currency, accountNumber, balanceAfter.toPlainString()));
            notification.put("timestamp", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("depositId", depositId);
            data.put("amount", amount);
            data.put("currency", currency);
            data.put("accountNumber", accountNumber);
            data.put("balanceAfter", balanceAfter);
            notification.put("data", data);

            // Add trace context
            Map<String, Object> trace = new HashMap<>();
            trace.put("traceId", Span.current().getSpanContext().getTraceId());
            trace.put("spanId", Span.current().getSpanContext().getSpanId());
            notification.put("trace", trace);

            publish(userId, notification);
            log.info("Published DEPOSIT_SUCCESS notification for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to publish deposit completed notification", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    public void publishWithdrawalCompleted(String userId, String withdrawalId,
                                           BigDecimal amount, String currency,
                                           String accountNumber, BigDecimal balanceAfter) {
        Span span = tracer.spanBuilder("notification.publish.withdrawal_completed")
                .startSpan();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationId", UUID.randomUUID().toString());
            notification.put("userId", userId);
            notification.put("type", "WITHDRAWAL_SUCCESS");
            notification.put("title", "提款成功");
            notification.put("message", String.format("已成功從帳戶 %s 提取 %s %s，餘額: %s",
                    accountNumber, amount.toPlainString(), currency, balanceAfter.toPlainString()));
            notification.put("timestamp", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("withdrawalId", withdrawalId);
            data.put("amount", amount);
            data.put("currency", currency);
            data.put("accountNumber", accountNumber);
            data.put("balanceAfter", balanceAfter);
            notification.put("data", data);

            // Add trace context
            Map<String, Object> trace = new HashMap<>();
            trace.put("traceId", Span.current().getSpanContext().getTraceId());
            trace.put("spanId", Span.current().getSpanContext().getSpanId());
            notification.put("trace", trace);

            publish(userId, notification);
            log.info("Published WITHDRAWAL_SUCCESS notification for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to publish withdrawal completed notification", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    public void publishWithdrawalFailed(String userId, String withdrawalId,
                                        BigDecimal amount, String currency,
                                        String reason) {
        Span span = tracer.spanBuilder("notification.publish.withdrawal_failed")
                .startSpan();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationId", UUID.randomUUID().toString());
            notification.put("userId", userId);
            notification.put("type", "WITHDRAWAL_FAILED");
            notification.put("title", "提款失敗");
            notification.put("message", String.format("提款 %s %s 失敗：%s",
                    amount.toPlainString(), currency, reason));
            notification.put("timestamp", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("withdrawalId", withdrawalId);
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
            log.info("Published WITHDRAWAL_FAILED notification for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to publish withdrawal failed notification", e);
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
