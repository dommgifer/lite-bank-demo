package com.litebank.analytics.query.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataResponse {

    private String period;
    private List<TrendPoint> trends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal totalBalance;
        private BigDecimal twdBalance;
        private BigDecimal foreignBalanceInTwd;
        private List<CurrencyBalance> byCurrency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyBalance {
        private String currency;
        private BigDecimal balance;
        private BigDecimal balanceInTwd;
    }
}
