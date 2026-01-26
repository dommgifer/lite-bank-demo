package com.litebank.analytics.processor.service;

import com.litebank.analytics.processor.model.entity.DailySummary;
import com.litebank.analytics.processor.model.event.TransactionCreatedEvent;
import com.litebank.analytics.processor.repository.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailySummaryService {

    private final DailySummaryRepository dailySummaryRepository;

    @Transactional
    public void updateDailySummary(TransactionCreatedEvent event) {
        TransactionCreatedEvent.TransactionPayload payload = event.getPayload();
        LocalDate transactionDate = payload.getCreatedAt().toLocalDate();

        DailySummary summary = dailySummaryRepository
                .findByUserIdAndDateAndCurrency(payload.getUserId(), transactionDate, payload.getCurrency())
                .orElseGet(() -> DailySummary.builder()
                        .userId(payload.getUserId())
                        .summaryDate(transactionDate)
                        .currency(payload.getCurrency())
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpense(BigDecimal.ZERO)
                        .transactionCount(0)
                        .build());

        // Update totals based on transaction type
        if (event.isIncome()) {
            summary.setTotalIncome(summary.getTotalIncome().add(payload.getAmount()));
        } else if (event.isExpense()) {
            summary.setTotalExpense(summary.getTotalExpense().add(payload.getAmount()));
        }

        // Increment transaction count
        summary.setTransactionCount(summary.getTransactionCount() + 1);

        // Update ending balance
        summary.setEndingBalance(payload.getBalanceAfter());

        dailySummaryRepository.save(summary);

        log.info("Daily summary updated: userId={}, date={}, currency={}, income={}, expense={}, count={}",
                payload.getUserId(),
                transactionDate,
                payload.getCurrency(),
                summary.getTotalIncome(),
                summary.getTotalExpense(),
                summary.getTransactionCount());
    }
}
