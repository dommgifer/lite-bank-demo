package com.litebank.transferservice.controller;

import com.litebank.transferservice.dto.ApiResponse;
import com.litebank.transferservice.dto.TransferRequest;
import com.litebank.transferservice.dto.TransferResponse;
import com.litebank.transferservice.service.TransferService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final Tracer tracer;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @Valid @RequestBody TransferRequest request
    ) {
        Span span = tracer.spanBuilder("POST /api/v1/transfers").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();
            span.setAttribute("from.account.id", request.getFromAccountId());
            span.setAttribute("to.account.id", request.getToAccountId());
            span.setAttribute("amount", request.getAmount().toString());

            TransferResponse response = transferService.transfer(request, traceId);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    record HealthStatus(String status, String timestamp, String service) {}
}
