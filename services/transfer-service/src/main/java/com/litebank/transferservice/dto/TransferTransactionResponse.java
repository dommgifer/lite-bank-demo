package com.litebank.transferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO from Transaction Service's transfer API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransactionResponse {
    private Long sourceTransactionId;
    private Long destinationTransactionId;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;
    private String referenceId;
    private String description;
    private String traceId;
    private LocalDateTime createdAt;
}
