package com.litebank.exchangeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeTransactionRequest {
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal sourceAmount;
    private String sourceCurrency;
    private String destinationCurrency;
    private String referenceId;
    private String description;
}
