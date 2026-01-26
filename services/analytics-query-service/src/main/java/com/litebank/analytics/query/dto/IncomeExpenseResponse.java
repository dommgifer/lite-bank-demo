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
public class IncomeExpenseResponse {

    private String period;  // e.g., "2024-01" or "2024-01 to 2024-06"
    private List<MonthlySummaryItem> monthly;
    private TotalSummary total;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySummaryItem {
        private String yearMonth;
        private String currency;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal netChange;
        private BigDecimal savingsRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalSummary {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netChange;
        private BigDecimal averageSavingsRate;
    }
}
