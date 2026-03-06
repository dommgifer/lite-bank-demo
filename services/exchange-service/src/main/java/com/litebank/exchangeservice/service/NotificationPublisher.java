package com.litebank.exchangeservice.service;

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

    public void publishExchangeCompleted(String userId, String exchangeId,
                                         BigDecimal sourceAmount, String sourceCurrency,
                                         BigDecimal destinationAmount, String destinationCurrency,
                                         BigDecimal exchangeRate) {
        Span span = tracer.spanBuilder("notification.publish.exchange_completed")
                .startSpan();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("notificationId", UUID.randomUUID().toString());
            notification.put("userId", userId);
            notification.put("type", "EXCHANGE_SUCCESS");
            notification.put("title", "換匯成功");
            notification.put("message", String.format("已成功將 %s %s 換匯為 %s %s（匯率：%s）",
                    sourceAmount.toPlainString(), sourceCurrency,
                    destinationAmount.toPlainString(), destinationCurrency,
                    exchangeRate.toPlainString()));
            notification.put("timestamp", Instant.now().toString());

            Map<String, Object> data = new HashMap<>();
            data.put("exchangeId", exchangeId);
            data.put("sourceAmount", sourceAmount);
            data.put("sourceCurrency", sourceCurrency);
            data.put("destinationAmount", destinationAmount);
            data.put("destinationCurrency", destinationCurrency);
            data.put("exchangeRate", exchangeRate);
            notification.put("data", data);

            Map<String, Object> trace = new HashMap<>();
            trace.put("traceId", Span.current().getSpanContext().getTraceId());
            trace.put("spanId", Span.current().getSpanContext().getSpanId());
            notification.put("trace", trace);

            publish(userId, notification);
            log.info("Published EXCHANGE_SUCCESS notification for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to publish exchange completed notification", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    private void publish(String userId, Map<String, Object> notification) throws JsonProcessingException {
        String message = objectMapper.writeValueAsString(notification);

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, userId, message);

        Map<String, String> headers = new HashMap<>();
        propagator.inject(Context.current(), headers, SETTER);
        headers.forEach((key, value) ->
                record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));

        kafkaTemplate.send(record);
    }
}
