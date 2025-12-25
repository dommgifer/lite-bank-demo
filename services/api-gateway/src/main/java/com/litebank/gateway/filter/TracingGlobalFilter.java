package com.litebank.gateway.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TracingGlobalFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = tracer.spanBuilder("API Gateway Request")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            ServerHttpRequest request = exchange.getRequest();

            // Add span attributes
            span.setAttribute("http.method", request.getMethod().toString());
            span.setAttribute("http.url", request.getURI().toString());
            span.setAttribute("http.path", request.getPath().toString());
            span.setAttribute("component", "api-gateway");

            // Get trace ID
            String traceId = span.getSpanContext().getTraceId();
            String spanId = span.getSpanContext().getSpanId();
            String traceFlags = span.getSpanContext().getTraceFlags().asHex();

            // Create traceparent header (W3C Trace Context format)
            String traceparent = String.format("00-%s-%s-%s", traceId, spanId, traceFlags);

            log.debug("Trace Context - TraceID: {}, SpanID: {}, Path: {}", traceId, spanId, request.getPath());

            // Generate request ID if not present
            String requestId = request.getHeaders().getFirst("X-Request-ID");
            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
            }

            span.setAttribute("request.id", requestId);

            // Modify request to add trace headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("traceparent", traceparent)
                    .header("X-Trace-Id", traceId)
                    .header("X-Request-ID", requestId)
                    .build();

            // Modify response to add trace headers
            exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
            exchange.getResponse().getHeaders().add("X-Request-ID", requestId);

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .doOnSuccess(aVoid -> {
                        span.setAttribute("http.status_code", exchange.getResponse().getStatusCode().value());
                        span.setStatus(StatusCode.OK);
                        log.debug("Request completed - TraceID: {}, Status: {}", traceId, exchange.getResponse().getStatusCode());
                    })
                    .doOnError(error -> {
                        span.setAttribute("error", true);
                        span.setAttribute("error.message", error.getMessage());
                        span.recordException(error);
                        span.setStatus(StatusCode.ERROR, error.getMessage());
                        log.error("Request failed - TraceID: {}, Error: {}", traceId, error.getMessage());
                    })
                    .doFinally(signalType -> span.end());

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.end();
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
