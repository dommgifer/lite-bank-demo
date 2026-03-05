package com.litebank.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.litebank.notification.dto.NotificationEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final TextMapPropagator propagator;

    private static final TextMapGetter<Map<String, String>> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    @KafkaListener(topics = "banking.notifications", groupId = "notification-service-group")
    public void consume(ConsumerRecord<String, String> record) {
        // 從 Kafka headers 提取 trace context
        Context extractedContext = extractTraceContext(record.headers());

        Span span = tracer.spanBuilder("notification.consume")
                .setParent(extractedContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", record.topic())
                .setAttribute("messaging.kafka.partition", record.partition())
                .startSpan();

        try {
            log.debug("Received Kafka message: {}", record.value());

            // 解析訊息
            NotificationEvent event = objectMapper.readValue(record.value(), NotificationEvent.class);

            span.setAttribute("notification.type", event.getType().toString());
            span.setAttribute("notification.userId", event.getUserId());

            // 使用 NotificationService 處理（Write-first + SSE push）
            var notification = notificationService.createAndSend(event);

            span.setAttribute("notification.id", notification.getId());
            span.setAttribute("notification.persisted", true);

            log.info("Notification processed: id={}, userId={}, type={}",
                    notification.getId(), event.getUserId(), event.getType());

        } catch (Exception e) {
            log.error("Failed to process notification message", e);
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    private Context extractTraceContext(Headers headers) {
        Map<String, String> headerMap = new HashMap<>();
        for (Header header : headers) {
            headerMap.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
        }
        return propagator.extract(Context.current(), headerMap, GETTER);
    }
}
