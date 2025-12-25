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
public class ExchangeResponse {

    private String exchangeId;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal sourceAmount;
    private String sourceCurrency;
    private BigDecimal destinationAmount;
    private String destinationCurrency;
    private BigDecimal exchangeRate;
    private String status;
    private Long sourceTransactionId;
    private Long destinationTransactionId;
    private String description;
    private String traceId;
    private LocalDateTime createdAt;
}
