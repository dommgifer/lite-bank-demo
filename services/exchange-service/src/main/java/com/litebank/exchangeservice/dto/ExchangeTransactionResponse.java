package com.litebank.exchangeservice.dto;

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
public class ExchangeTransactionResponse {
    private Long sourceTransactionId;
    private Long destinationTransactionId;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal sourceAmount;
    private String sourceCurrency;
    private BigDecimal destinationAmount;
    private String destinationCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal sourceBalanceAfter;
    private BigDecimal destinationBalanceAfter;
    private String referenceId;
    private String description;
    private String traceId;
    private LocalDateTime createdAt;
}
