package com.litebank.userservice.service;

import com.litebank.userservice.dto.LoginRequest;
import com.litebank.userservice.dto.LoginResponse;
import com.litebank.userservice.entity.User;
import com.litebank.userservice.exception.AuthenticationException;
import com.litebank.userservice.repository.UserRepository;
import com.litebank.userservice.util.JwtUtil;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Tracer tracer;

    public LoginResponse login(LoginRequest request) {
        // Start OpenTelemetry span
        Span span = tracer.spanBuilder("AuthService.login")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Add span attributes
            span.setAttribute("user.username", request.getUsername());
            span.setAttribute("operation", "login");

            log.info("Login attempt for username: {}", request.getUsername());

            // Find user by username
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found: {}", request.getUsername());
                        span.setStatus(StatusCode.ERROR, "User not found");
                        span.recordException(new AuthenticationException("Invalid credentials"));
                        throw new AuthenticationException("Invalid credentials");
                    });

            log.debug("Found user: {}, passwordHash length: {}, hash prefix: {}",
                    user.getUsername(),
                    user.getPasswordHash() != null ? user.getPasswordHash().length() : 0,
                    user.getPasswordHash() != null && user.getPasswordHash().length() > 10 ? user.getPasswordHash().substring(0, 10) : "N/A");

            // Verify password
            boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
            log.debug("Password verification result: {}", passwordMatches);

            if (!passwordMatches) {
                log.error("Password mismatch for user: {}", request.getUsername());
                span.setStatus(StatusCode.ERROR, "Invalid password");
                span.recordException(new AuthenticationException("Invalid credentials"));
                throw new AuthenticationException("Invalid credentials");
            }

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());

            // Add success attributes
            span.setAttribute("user.id", user.getUserId());
            span.setAttribute("auth.success", true);
            span.setStatus(StatusCode.OK);

            log.info("Login successful for user: {} (ID: {})", user.getUsername(), user.getUserId());

            return LoginResponse.builder()
                    .token(token)
                    .expiresIn(jwtUtil.getExpiration())
                    .username(user.getUsername())
                    .userId(user.getUserId())
                    .build();

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
