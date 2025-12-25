package com.litebank.transactionservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 換匯請求（原子性操作：同時扣款和入帳不同幣別）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRequest {

    @NotNull(message = "Source account ID is required")
    private Long sourceAccountId;

    @NotNull(message = "Destination account ID is required")
    private Long destinationAccountId;

    @NotNull(message = "Source amount is required")
    @DecimalMin(value = "0.01", message = "Source amount must be greater than 0")
    private BigDecimal sourceAmount;

    @NotBlank(message = "Source currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Source currency must be a 3-letter code")
    private String sourceCurrency;

    @NotNull(message = "Destination amount is required")
    @DecimalMin(value = "0.01", message = "Destination amount must be greater than 0")
    private BigDecimal destinationAmount;

    @NotBlank(message = "Destination currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Destination currency must be a 3-letter code")
    private String destinationCurrency;

    @NotNull(message = "Exchange rate is required")
    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than 0")
    private BigDecimal exchangeRate;

    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    private String referenceId;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
