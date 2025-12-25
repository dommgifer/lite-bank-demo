package com.litebank.transactionservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(DEPOSIT|WITHDRAWAL|TRANSFER_OUT|TRANSFER_IN|EXCHANGE_OUT|EXCHANGE_IN|FEE)$",
            message = "Transaction type must be one of: DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN, EXCHANGE_OUT, EXCHANGE_IN, FEE")
    private String transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code (e.g., USD, EUR, TWD)")
    private String currency;

    @NotNull(message = "Balance after transaction is required")
    @DecimalMin(value = "0.00", message = "Balance after must be non-negative")
    private BigDecimal balanceAfter;

    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    private String referenceId;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Map<String, Object> metadata;
}
