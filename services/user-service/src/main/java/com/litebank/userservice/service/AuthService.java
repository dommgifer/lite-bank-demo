package com.litebank.userservice.service;

import com.litebank.userservice.client.AccountServiceClient;
import com.litebank.userservice.dto.LoginRequest;
import com.litebank.userservice.dto.LoginResponse;
import com.litebank.userservice.dto.RegisterRequest;
import com.litebank.userservice.dto.RegisterResponse;
import com.litebank.userservice.entity.User;
import com.litebank.userservice.exception.AuthenticationException;
import com.litebank.userservice.exception.DuplicateUserException;
import com.litebank.userservice.exception.RegistrationException;
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
    private final AccountServiceClient accountServiceClient;

    public LoginResponse login(LoginRequest request) {
        // Start OpenTelemetry span
        Span span = tracer.spanBuilder("AuthService.login")
                .setParent(io.opentelemetry.context.Context.current())
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

    public RegisterResponse register(RegisterRequest request) {
        Span span = tracer.spanBuilder("AuthService.register")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("user.username", request.getUsername());
            span.setAttribute("operation", "register");

            log.info("Registration attempt for username: {}", request.getUsername());

            // 檢查 username 是否重複
            if (userRepository.existsByUsername(request.getUsername())) {
                log.error("Username already exists: {}", request.getUsername());
                span.setStatus(StatusCode.ERROR, "Username already exists");
                throw new DuplicateUserException("Username already exists");
            }

            // 檢查 email 是否重複
            if (userRepository.existsByEmail(request.getEmail())) {
                log.error("Email already exists: {}", request.getEmail());
                span.setStatus(StatusCode.ERROR, "Email already exists");
                throw new DuplicateUserException("Email already exists");
            }

            // 建立用戶
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .build();

            user = userRepository.save(user);
            log.info("User created: {} (ID: {})", user.getUsername(), user.getUserId());

            // 呼叫 Account Service 建立預設 TWD 帳戶（含重試）
            boolean accountCreated = createDefaultAccountWithRetry(user.getUserId(), 3);

            if (!accountCreated) {
                // 補償操作：刪除剛建立的用戶
                log.error("Failed to create default account for userId: {}, rolling back user creation", user.getUserId());
                userRepository.delete(user);
                span.setStatus(StatusCode.ERROR, "Registration failed");
                throw new RegistrationException("Registration failed, please try again later");
            }

            span.setAttribute("user.id", user.getUserId());
            span.setStatus(StatusCode.OK);
            log.info("Registration successful for user: {} (ID: {})", user.getUsername(), user.getUserId());

            return RegisterResponse.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .createdAt(user.getCreatedAt())
                    .build();

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private boolean createDefaultAccountWithRetry(Long userId, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                accountServiceClient.createDefaultAccount(userId);
                return true;
            } catch (Exception e) {
                log.warn("Create default account failed, retry {}/{}: {}", i + 1, maxRetries, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(100L * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }
}
