package com.litebank.userservice.controller;

import com.litebank.userservice.dto.ApiResponse;
import com.litebank.userservice.dto.LoginRequest;
import com.litebank.userservice.dto.LoginResponse;
import com.litebank.userservice.service.AuthService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final Tracer tracer;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Start OpenTelemetry span
        Span span = tracer.spanBuilder("POST /api/v1/auth/login")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Add span attributes
            span.setAttribute("http.method", "POST");
            span.setAttribute("http.route", "/api/v1/auth/login");
            span.setAttribute("user.username", request.getUsername());

            // Call service
            LoginResponse response = authService.login(request);

            // Get trace ID for response
            String traceId = span.getSpanContext().getTraceId();

            // Return success response with X-Trace-Id header
            return ResponseEntity.ok()
                    .header("X-Trace-Id", traceId)
                    .body(ApiResponse.success(response, traceId));

        } finally {
            span.end();
        }
    }

}
