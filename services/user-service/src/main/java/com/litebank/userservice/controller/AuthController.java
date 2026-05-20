package com.litebank.userservice.controller;

import com.litebank.userservice.dto.ApiResponse;
import com.litebank.userservice.dto.LoginRequest;
import com.litebank.userservice.dto.LoginResponse;
import com.litebank.userservice.dto.RegisterRequest;
import com.litebank.userservice.dto.RegisterResponse;
import com.litebank.userservice.service.AuthService;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Span.current().setAttribute("http.route", "/api/v1/auth/login");
        Span.current().setAttribute("user.username", request.getUsername());

        LoginResponse response = authService.login(request);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.ok()
                .body(ApiResponse.success(response, traceId));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        Span.current().setAttribute("http.route", "/api/v1/auth/register");
        Span.current().setAttribute("user.username", request.getUsername());

        RegisterResponse response = authService.register(request);

        String traceId = Span.current().getSpanContext().getTraceId();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, traceId));
    }

}
