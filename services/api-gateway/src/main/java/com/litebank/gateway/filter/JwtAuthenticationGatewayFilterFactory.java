package com.litebank.gateway.filter;

import com.litebank.gateway.util.JwtUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Tracer tracer;

    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Span span = tracer.spanBuilder("JwtAuthenticationFilter")
                    .startSpan();

            try {
                ServerHttpRequest request = exchange.getRequest();
                String path = request.getPath().toString();

                span.setAttribute("http.path", path);

                // Check if path is in excluded paths
                if (config.getExcludePaths() != null) {
                    for (String excludedPath : config.getExcludePaths().split(",")) {
                        if (path.contains(excludedPath.trim())) {
                            log.debug("Path {} is excluded from JWT validation", path);
                            span.setAttribute("jwt.validation", "skipped");
                            return chain.filter(exchange);
                        }
                    }
                }

                // Extract JWT token from Authorization header
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("Missing or invalid Authorization header for path: {}", path);
                    span.setAttribute("jwt.validation", "failed");
                    span.setAttribute("error.reason", "missing_token");
                    return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                // Validate JWT token
                if (!jwtUtil.validateToken(token)) {
                    log.warn("Invalid JWT token for path: {}", path);
                    span.setAttribute("jwt.validation", "failed");
                    span.setAttribute("error.reason", "invalid_token");
                    return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
                }

                // Extract user ID from token and add to request headers
                String userId = jwtUtil.extractUserId(token);
                span.setAttribute("user.id", userId);
                span.setAttribute("jwt.validation", "success");

                log.debug("JWT validation successful for user: {}, path: {}", userId, path);

                // Add user ID to request headers for downstream services
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Error during JWT validation: {}", e.getMessage());
                span.setAttribute("jwt.validation", "error");
                span.setAttribute("error.message", e.getMessage());
                span.recordException(e);
                return onError(exchange, "Authentication error", HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                span.end();
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String errorResponse = String.format(
                "{\"success\":false,\"error\":{\"message\":\"%s\",\"status\":%d}}",
                errorMessage, httpStatus.value()
        );

        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorResponse.getBytes())));
    }

    public static class Config {
        private String excludePaths;

        public String getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(String excludePaths) {
            this.excludePaths = excludePaths;
        }
    }
}
