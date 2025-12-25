package com.litebank.gateway.filter;

import com.litebank.gateway.util.JwtUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationGatewayFilterFactoryTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthenticationGatewayFilterFactory();
        // Use reflection to set private fields for testing
        org.springframework.test.util.ReflectionTestUtils.setField(filterFactory, "jwtUtil", jwtUtil);
        org.springframework.test.util.ReflectionTestUtils.setField(filterFactory, "tracer", tracer);

        // Mock tracer to return span
        when(tracer.spanBuilder(anyString())).thenReturn(mock(io.opentelemetry.api.trace.SpanBuilder.class));
        io.opentelemetry.api.trace.SpanBuilder spanBuilder = tracer.spanBuilder(anyString());
        when(spanBuilder.startSpan()).thenReturn(span);

        // Mock span methods
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        when(span.setAttribute(anyString(), anyBoolean())).thenReturn(span);
        doNothing().when(span).end();
    }

    @Test
    void apply_withValidToken_shouldAllowRequest() {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractUserId(validToken)).thenReturn(userId);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil).validateToken(validToken);
        verify(jwtUtil).extractUserId(validToken);
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void apply_withMissingAuthorizationHeader_shouldReturn401() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apply_withInvalidAuthorizationHeaderFormat_shouldReturn401() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apply_withInvalidToken_shouldReturn401() {
        // Given
        String invalidToken = "invalid.jwt.token";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(jwtUtil).validateToken(invalidToken);
        verify(jwtUtil, never()).extractUserId(anyString());
        verify(chain, never()).filter(any());
    }

    @Test
    void apply_withExpiredToken_shouldReturn401() {
        // Given
        String expiredToken = "expired.jwt.token";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(expiredToken)).thenReturn(false);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apply_withExcludedPath_shouldBypassAuthentication() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/login")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        config.setExcludePaths("/api/v1/auth/login,/api/v1/auth/register");
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void apply_withExcludedPath_register_shouldBypassAuthentication() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/auth/register")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        config.setExcludePaths("/api/v1/auth/login,/api/v1/auth/register");
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil, never()).validateToken(anyString());
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void apply_withNonExcludedPath_shouldRequireAuthentication() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        config.setExcludePaths("/api/v1/auth/login,/api/v1/auth/register");
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apply_withValidationException_shouldReturn500() {
        // Given
        String validToken = "valid.jwt.token";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(validToken)).thenThrow(new RuntimeException("Validation error"));

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apply_withNullExcludePaths_shouldStillWork() {
        // Given
        String validToken = "valid.jwt.token";
        String userId = "user123";

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtUtil.validateToken(validToken)).thenReturn(true);
        when(jwtUtil.extractUserId(validToken)).thenReturn(userId);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        config.setExcludePaths(null); // Explicitly set to null
        GatewayFilter filter = filterFactory.apply(config);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtUtil).validateToken(validToken);
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void config_setAndGetExcludePaths_shouldWork() {
        // Given
        JwtAuthenticationGatewayFilterFactory.Config config = new JwtAuthenticationGatewayFilterFactory.Config();
        String excludePaths = "/api/v1/auth/login,/api/v1/auth/register";

        // When
        config.setExcludePaths(excludePaths);
        String result = config.getExcludePaths();

        // Then
        assertEquals(excludePaths, result);
    }
}
