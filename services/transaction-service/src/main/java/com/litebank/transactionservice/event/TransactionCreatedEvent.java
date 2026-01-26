package com.litebank.transactionservice.event;

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
}
