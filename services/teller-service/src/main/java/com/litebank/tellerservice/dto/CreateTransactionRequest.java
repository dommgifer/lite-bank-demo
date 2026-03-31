package com.litebank.tellerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {
    private Long accountId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal balanceAfter;
    private String referenceId;
    private String description;
}
