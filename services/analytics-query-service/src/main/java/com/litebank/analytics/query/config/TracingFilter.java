package com.litebank.analytics.query.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

@Component
@Order(1)
@RequiredArgsConstructor
public class TracingFilter implements Filter {

    private final Tracer tracer;

    private static final TextMapGetter<HttpServletRequest> getter = new TextMapGetter<>() {
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

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Extract context from incoming request
        Context extractedContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), httpRequest, getter);

        // Create span with extracted context as parent
        Span span = tracer.spanBuilder(httpRequest.getMethod() + " " + httpRequest.getRequestURI())
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Add request attributes
            span.setAttribute("http.method", httpRequest.getMethod());
            span.setAttribute("http.url", httpRequest.getRequestURL().toString());
            span.setAttribute("http.target", httpRequest.getRequestURI());

            // Add trace ID to response header
            SpanContext spanContext = span.getSpanContext();
            if (spanContext.isValid()) {
                httpResponse.setHeader("X-Trace-Id", spanContext.getTraceId());
                // Put traceId and spanId into MDC for logging
                MDC.put("traceId", spanContext.getTraceId());
                MDC.put("spanId", spanContext.getSpanId());
            }

            try {
                chain.doFilter(request, response);

                span.setAttribute("http.status_code", httpResponse.getStatus());
            } finally {
                // Clean up MDC
                MDC.remove("traceId");
                MDC.remove("spanId");
            }
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
