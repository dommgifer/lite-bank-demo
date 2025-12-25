package com.litebank.transactionservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 出款請求 (WITHDRAWAL, TRANSFER_OUT, EXCHANGE_OUT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitRequest {

    @NotNull(message = "Account ID is required")
    private Long accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter code (e.g., USD, EUR, TWD)")
    private String currency;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(WITHDRAWAL|TRANSFER_OUT|EXCHANGE_OUT)$",
            message = "Transaction type must be one of: WITHDRAWAL, TRANSFER_OUT, EXCHANGE_OUT")
    private String transactionType;

    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    private String referenceId;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
