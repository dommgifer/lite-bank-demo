package com.litebank.depositwithdrawalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitRequest {
    private Long accountId;
    private BigDecimal amount;
    private String currency;
    private String referenceId;
    private String description;
}
