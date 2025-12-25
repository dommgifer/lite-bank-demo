package com.litebank.transactionservice.controller;

import com.litebank.transactionservice.dto.*;
import com.litebank.transactionservice.entity.Transaction;
import com.litebank.transactionservice.service.TransactionService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final Tracer tracer;

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable Long transactionId) {
        Span span = tracer.spanBuilder("GET /api/v1/transactions/{transactionId}").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("transaction.id", transactionId);

            Transaction transaction = transactionService.getTransactionById(transactionId);
            TransactionResponse response = TransactionResponse.fromEntity(transaction);

            String traceId = span.getSpanContext().getTraceId();
            return ResponseEntity.ok(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> queryTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String referenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Span span = tracer.spanBuilder("GET /api/v1/transactions").startSpan();
        try (Scope scope = span.makeCurrent()) {
            TransactionQueryParams params = TransactionQueryParams.builder()
                    .accountId(accountId)
                    .transactionType(transactionType)
                    .startDate(startDate)
                    .endDate(endDate)
                    .referenceId(referenceId)
                    .page(page)
                    .size(size)
                    .build();

            Page<Transaction> transactionPage = transactionService.queryTransactions(params);
            List<TransactionResponse> content = transactionPage.getContent().stream()
                    .map(TransactionResponse::fromEntity)
                    .collect(Collectors.toList());

            PageResponse<TransactionResponse> pageResponse = new PageResponse<>(
                    content,
                    transactionPage.getNumber(),
                    transactionPage.getSize(),
                    transactionPage.getTotalElements(),
                    transactionPage.getTotalPages()
            );

            String traceId = span.getSpanContext().getTraceId();
            return ResponseEntity.ok(ApiResponse.success(pageResponse, traceId));
        } finally {
            span.end();
        }
    }

    @GetMapping("/trace/{traceId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByTraceId(@PathVariable String traceId) {
        Span span = tracer.spanBuilder("GET /api/v1/transactions/trace/{traceId}").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("trace.id", traceId);

            List<Transaction> transactions = transactionService.getTransactionsByTraceId(traceId);
            List<TransactionResponse> response = transactions.stream()
                    .map(TransactionResponse::fromEntity)
                    .collect(Collectors.toList());

            String currentTraceId = span.getSpanContext().getTraceId();
            return ResponseEntity.ok(ApiResponse.success(response, currentTraceId));
        } finally {
            span.end();
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        Span span = tracer.spanBuilder("POST /api/v1/transactions").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();

            Transaction transaction = transactionService.createTransaction(request, traceId);
            TransactionResponse response = TransactionResponse.fromEntity(transaction);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    /**
     * 入款 API (DEPOSIT, TRANSFER_IN, EXCHANGE_IN)
     * 原子性操作：更新帳戶餘額 + 記錄交易
     */
    @PostMapping("/credit")
    public ResponseEntity<ApiResponse<TransactionResponse>> credit(
            @Valid @RequestBody CreditRequest request
    ) {
        Span span = tracer.spanBuilder("POST /api/v1/transactions/credit").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();

            Transaction transaction = transactionService.credit(request, traceId);
            TransactionResponse response = TransactionResponse.fromEntity(transaction);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    /**
     * 出款 API (WITHDRAWAL, TRANSFER_OUT, EXCHANGE_OUT)
     * 原子性操作：驗證餘額 + 更新帳戶餘額 + 記錄交易
     */
    @PostMapping("/debit")
    public ResponseEntity<ApiResponse<TransactionResponse>> debit(
            @Valid @RequestBody DebitRequest request
    ) {
        Span span = tracer.spanBuilder("POST /api/v1/transactions/debit").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();

            Transaction transaction = transactionService.debit(request, traceId);
            TransactionResponse response = TransactionResponse.fromEntity(transaction);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    /**
     * 轉帳 API (同幣別)
     * 原子性操作：扣款 + 入帳 + 記錄兩筆交易
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @Valid @RequestBody TransferRequest request
    ) {
        Span span = tracer.spanBuilder("POST /api/v1/transactions/transfer").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();

            List<Transaction> transactions = transactionService.transfer(request, traceId);

            // transactions[0] is source (OUT), transactions[1] is destination (IN)
            TransferResponse response = TransferResponse.builder()
                    .sourceTransactionId(transactions.get(0).getTransactionId())
                    .destinationTransactionId(transactions.get(1).getTransactionId())
                    .sourceAccountId(request.getSourceAccountId())
                    .destinationAccountId(request.getDestinationAccountId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .sourceBalanceAfter(transactions.get(0).getBalanceAfter())
                    .destinationBalanceAfter(transactions.get(1).getBalanceAfter())
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .createdAt(transactions.get(0).getCreatedAt())
                    .build();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    /**
     * 換匯 API (跨幣別)
     * 原子性操作：扣款 + 入帳 + 記錄兩筆交易
     */
    @PostMapping("/exchange")
    public ResponseEntity<ApiResponse<ExchangeResponse>> exchange(
            @Valid @RequestBody ExchangeRequest request
    ) {
        Span span = tracer.spanBuilder("POST /api/v1/transactions/exchange").startSpan();
        try (Scope scope = span.makeCurrent()) {
            String traceId = span.getSpanContext().getTraceId();

            List<Transaction> transactions = transactionService.exchange(request, traceId);

            // transactions[0] is source (OUT), transactions[1] is destination (IN)
            ExchangeResponse response = ExchangeResponse.builder()
                    .sourceTransactionId(transactions.get(0).getTransactionId())
                    .destinationTransactionId(transactions.get(1).getTransactionId())
                    .sourceAccountId(request.getSourceAccountId())
                    .destinationAccountId(request.getDestinationAccountId())
                    .sourceAmount(request.getSourceAmount())
                    .sourceCurrency(request.getSourceCurrency())
                    .destinationAmount(request.getDestinationAmount())
                    .destinationCurrency(request.getDestinationCurrency())
                    .exchangeRate(request.getExchangeRate())
                    .sourceBalanceAfter(transactions.get(0).getBalanceAfter())
                    .destinationBalanceAfter(transactions.get(1).getBalanceAfter())
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .createdAt(transactions.get(0).getCreatedAt())
                    .build();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, traceId));
        } finally {
            span.end();
        }
    }

    record HealthStatus(String status, String timestamp, String service) {}

    record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
