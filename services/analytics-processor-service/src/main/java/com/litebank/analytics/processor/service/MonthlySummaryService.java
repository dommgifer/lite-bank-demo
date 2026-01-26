package com.litebank.analytics.processor.service;

import com.litebank.analytics.processor.model.entity.MonthlySummary;
import com.litebank.analytics.processor.model.event.TransactionCreatedEvent;
import com.litebank.analytics.processor.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlySummaryService {

    private final MonthlySummaryRepository monthlySummaryRepository;
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional
    public void updateMonthlySummary(TransactionCreatedEvent event) {
        TransactionCreatedEvent.TransactionPayload payload = event.getPayload();
        String yearMonth = payload.getCreatedAt().format(YEAR_MONTH_FORMATTER);

        MonthlySummary summary = monthlySummaryRepository
                .findByUserIdAndYearMonthAndCurrency(payload.getUserId(), yearMonth, payload.getCurrency())
                .orElseGet(() -> MonthlySummary.builder()
                        .userId(payload.getUserId())
                        .yearMonth(yearMonth)
                        .currency(payload.getCurrency())
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpense(BigDecimal.ZERO)
                        .netChange(BigDecimal.ZERO)
                        .build());

        // Update totals based on transaction type
        if (event.isIncome()) {
            summary.setTotalIncome(summary.getTotalIncome().add(payload.getAmount()));
        } else if (event.isExpense()) {
            summary.setTotalExpense(summary.getTotalExpense().add(payload.getAmount()));
        }

        // Calculate derived fields (net change and savings rate)
        summary.calculateDerivedFields();

        monthlySummaryRepository.save(summary);

        log.info("Monthly summary updated: userId={}, yearMonth={}, currency={}, income={}, expense={}, netChange={}, savingsRate={}%",
                payload.getUserId(),
                yearMonth,
                payload.getCurrency(),
                summary.getTotalIncome(),
                summary.getTotalExpense(),
                summary.getNetChange(),
                summary.getSavingsRate());
    }
}
