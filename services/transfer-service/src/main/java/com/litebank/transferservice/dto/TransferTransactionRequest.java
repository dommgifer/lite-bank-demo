package com.litebank.transferservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for calling Transaction Service's transfer API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransactionRequest {
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String referenceId;
    private String description;
}
