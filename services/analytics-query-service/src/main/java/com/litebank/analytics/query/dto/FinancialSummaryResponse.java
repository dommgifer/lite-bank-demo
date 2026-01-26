package com.litebank.analytics.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryResponse {

    private CurrentMonthSummary currentMonth;
    private Comparison comparison;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentMonthSummary {
        private List<CurrencySummary> byCurrency;
        private BigDecimal totalIncomeInTwd;
        private BigDecimal totalExpenseInTwd;
        private BigDecimal netChangeInTwd;
        private BigDecimal overallSavingsRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencySummary {
        private String currency;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netChange;
        private BigDecimal savingsRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comparison {
        private BigDecimal incomeChangePercent;
        private BigDecimal expenseChangePercent;
        private BigDecimal savingsRateChange;
    }
}
