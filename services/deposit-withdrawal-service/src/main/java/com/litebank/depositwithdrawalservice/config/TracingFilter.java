package com.litebank.depositwithdrawalservice.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

/**
 * Servlet Filter that extracts W3C Trace Context (traceparent header) from incoming HTTP requests
 * and sets it as the current context for proper trace propagation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class TracingFilter implements Filter {

    private final OpenTelemetry openTelemetry;

    private static final TextMapGetter<HttpServletRequest> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract trace context from incoming request headers
        Context extractedContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), httpRequest, GETTER);

        // Log for debugging
        String traceparent = httpRequest.getHeader("traceparent");
        if (traceparent != null) {
            log.debug("Extracted traceparent: {}", traceparent);
        }

        // Make the extracted context current for the duration of this request
        try (Scope scope = extractedContext.makeCurrent()) {
            // Put traceId and spanId into MDC for logging
            SpanContext spanContext = Span.current().getSpanContext();
            if (spanContext.isValid()) {
                MDC.put("traceId", spanContext.getTraceId());
                MDC.put("spanId", spanContext.getSpanId());
            }

            try {
                chain.doFilter(request, response);
            } finally {
                // Clean up MDC
                MDC.remove("traceId");
                MDC.remove("spanId");
            }
        }
    }
}
