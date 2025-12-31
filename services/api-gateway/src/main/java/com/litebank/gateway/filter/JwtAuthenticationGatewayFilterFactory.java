package com.litebank.gateway.filter;

import com.litebank.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);

    private final JwtUtil jwtUtil;

    @Autowired
    public JwtAuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        log.info("JwtAuthenticationGatewayFilterFactory initialized with JwtUtil");
    }

    @Override
    public GatewayFilter apply(Config config) {
        log.info("Creating GatewayFilter with excludePaths: {}", config.getExcludePaths());

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            log.info("JWT Filter processing request: {}", path);

            // Check if path is excluded
            if (config.getExcludePaths() != null) {
                for (String excludedPath : config.getExcludePaths().split(",")) {
                    if (path.contains(excludedPath.trim())) {
                        log.info("Path {} is excluded from JWT validation", path);
                        return chain.filter(exchange);
                    }
                }
            }

            // Extract JWT token
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
            }

            // Extract user ID
            String userId = jwtUtil.extractUserId(token);

            log.info("JWT validation successful for user: {}, path: {}", userId, path);

            // Add user ID to headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();

            // Continue the chain
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
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
