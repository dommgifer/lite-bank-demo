package com.litebank.gateway.filter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TracingGlobalFilterTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private SpanContext spanContext;

    @Mock
    private Scope scope;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private io.opentelemetry.api.trace.SpanBuilder spanBuilder;

    private TracingGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TracingGlobalFilter(tracer);

        // Mock tracer and span builder
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.getSpanContext()).thenReturn(spanContext);

        // Mock span context
        when(spanContext.getTraceId()).thenReturn("0123456789abcdef0123456789abcdef");
        when(spanContext.getSpanId()).thenReturn("0123456789abcdef");
        when(spanContext.getTraceFlags()).thenReturn(mock(io.opentelemetry.api.trace.TraceFlags.class));
        when(spanContext.getTraceFlags().asHex()).thenReturn("01");

        // Mock span methods
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        when(span.setAttribute(anyString(), anyInt())).thenReturn(span);
        when(span.setAttribute(anyString(), anyBoolean())).thenReturn(span);
        when(span.setStatus(any(StatusCode.class))).thenReturn(span);
        when(span.setStatus(any(StatusCode.class), anyString())).thenReturn(span);
        doNothing().when(span).recordException(any(Throwable.class));
        doNothing().when(span).end();
        doNothing().when(scope).close();
    }

    @Test
    void filter_withSuccessfulRequest_shouldSetOkStatus() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, "/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.OK);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(span).setAttribute("http.method", "GET");
        verify(span).setAttribute("http.path", "/api/v1/accounts");
        verify(span).setAttribute("component", "api-gateway");
        verify(span).setAttribute("http.status_code", 200);
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }

    @Test
    void filter_shouldAddTraceHeadersToResponse() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.POST, "/api/v1/transfers")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.CREATED);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-Request-ID"));
    }

    @Test
    void filter_shouldPropagateTraceparentHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(span).setAttribute("http.method", "GET");
        verify(span).setAttribute("component", "api-gateway");
    }

    @Test
    void filter_withExistingRequestId_shouldUseIt() {
        // Given
        String existingRequestId = "existing-request-id-12345";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header("X-Request-ID", existingRequestId)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(span).setAttribute("request.id", existingRequestId);
        assertEquals(existingRequestId, exchange.getResponse().getHeaders().getFirst("X-Request-ID"));
    }

    @Test
    void filter_withoutRequestId_shouldGenerateOne() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        String requestId = exchange.getResponse().getHeaders().getFirst("X-Request-ID");
        assertNotNull(requestId);
        verify(span).setAttribute(eq("request.id"), anyString());
    }

    @Test
    void filter_withError_shouldSetErrorStatus() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException error = new RuntimeException("Test error");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.error(error));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(span).setAttribute("error", true);
        verify(span).setAttribute("error.message", "Test error");
        verify(span).recordException(error);
        verify(span).setStatus(StatusCode.ERROR, "Test error");
        verify(span).end();
    }

    @Test
    void filter_with500Response_shouldStillSetOkStatus() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(span).setAttribute("http.status_code", 500);
        verify(span).setStatus(StatusCode.OK);
    }

    @Test
    void filter_shouldSetSpanAttributes() {
        // Given
        String testPath = "/api/v1/transactions/12345";

        MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.PUT, testPath)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(span).setAttribute("http.method", "PUT");
        verify(span).setAttribute("http.url", request.getURI().toString());
        verify(span).setAttribute("http.path", testPath);
        verify(span).setAttribute("component", "api-gateway");
    }

    @Test
    void getOrder_shouldReturnHighestPrecedence() {
        // When
        int order = filter.getOrder();

        // Then
        assertEquals(Integer.MIN_VALUE, order);
    }

    @Test
    void filter_shouldEndSpanAfterCompletion() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(span).end();
    }

    @Test
    void filter_withNullPointerException_shouldHandleGracefully() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        NullPointerException error = new NullPointerException("Null value encountered");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.error(error));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .expectError(NullPointerException.class)
                .verify();

        verify(span).recordException(error);
        verify(span).setStatus(StatusCode.ERROR, "Null value encountered");
        verify(span).end();
    }
}
