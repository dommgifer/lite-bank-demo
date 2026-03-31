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
public class CreditRequest {
    private Long accountId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String referenceId;
    private String description;
}
