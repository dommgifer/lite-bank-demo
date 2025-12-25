package com.litebank.transferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
}
