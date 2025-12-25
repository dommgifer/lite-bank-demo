package com.litebank.transactionservice.dto;

import com.litebank.transactionservice.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long transactionId;
    private Long accountId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private String referenceId;
    private String description;
    private String traceId;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;

    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .accountId(transaction.getAccountId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .balanceAfter(transaction.getBalanceAfter())
                .referenceId(transaction.getReferenceId())
                .description(transaction.getDescription())
                .traceId(transaction.getTraceId())
                .metadata(transaction.getMetadata())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
