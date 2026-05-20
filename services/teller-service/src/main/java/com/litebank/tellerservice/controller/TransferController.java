package com.litebank.tellerservice.controller;

import com.litebank.tellerservice.dto.ApiResponse;
import com.litebank.tellerservice.dto.TransferRequest;
import com.litebank.tellerservice.dto.TransferResponse;
import com.litebank.tellerservice.service.TransferService;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Valid @RequestBody TransferRequest request
    ) {
        Span.current().setAttribute("http.route", "/api/v1/transfers");
        Span.current().setAttribute("from.account.id", request.getFromAccountId());
        Span.current().setAttribute("to.account.id", request.getToAccountId());
        Span.current().setAttribute("amount", request.getAmount().toString());
        if (userId != null) {
            Span.current().setAttribute("user.id", userId);
        }

        String traceId = Span.current().getSpanContext().getTraceId();
        TransferResponse response = transferService.transfer(request, traceId, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, traceId));
    }
}
