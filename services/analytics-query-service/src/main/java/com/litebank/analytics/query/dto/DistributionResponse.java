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
public class DistributionResponse {

    private BigDecimal totalBalanceInTwd;
    private List<CurrencyDistribution> distribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyDistribution {
        private String currency;
        private BigDecimal balance;
        private BigDecimal balanceInTwd;
        private BigDecimal percentage;
    }
}
