package com.litebank.analytics.processor.model.event;

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
public class TransactionCreatedEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    private TransactionPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionPayload {
        private Long transactionId;
        private Long userId;
        private Long accountId;
        private BigDecimal amount;
        private String transactionType;  // DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, EXCHANGE_IN, EXCHANGE_OUT
        private String currency;
        private BigDecimal balanceAfter;
        private String description;
        private String traceId;
        private LocalDateTime createdAt;
    }

    /**
     * Determines if this transaction type represents income
     */
    public boolean isIncome() {
        if (payload == null || payload.getTransactionType() == null) {
            return false;
        }
        String type = payload.getTransactionType();
        return "DEPOSIT".equals(type) || "TRANSFER_IN".equals(type) || "EXCHANGE_IN".equals(type);
    }

    /**
     * Determines if this transaction type represents expense
     */
    public boolean isExpense() {
        if (payload == null || payload.getTransactionType() == null) {
            return false;
        }
        String type = payload.getTransactionType();
        return "WITHDRAWAL".equals(type) || "TRANSFER_OUT".equals(type) || "EXCHANGE_OUT".equals(type);
    }
}
