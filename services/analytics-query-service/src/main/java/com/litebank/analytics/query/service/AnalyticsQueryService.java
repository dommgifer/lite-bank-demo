package com.litebank.analytics.query.service;

import com.litebank.analytics.query.dto.*;
import com.litebank.analytics.query.model.entity.BalanceSnapshot;
import com.litebank.analytics.query.model.entity.MonthlySummary;
import com.litebank.analytics.query.repository.BalanceSnapshotRepository;
import com.litebank.analytics.query.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsQueryService {

    private final MonthlySummaryRepository monthlySummaryRepository;
    private final BalanceSnapshotRepository balanceSnapshotRepository;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // Exchange rates to TWD (same as frontend)
    private static final Map<String, BigDecimal> EXCHANGE_RATES_TO_TWD = Map.of(
            "TWD", BigDecimal.ONE,
            "USD", new BigDecimal("31.25"),
            "EUR", new BigDecimal("33.85"),
            "JPY", new BigDecimal("0.21"),
            "GBP", new BigDecimal("39.50")
    );

    /**
     * Get financial summary for the current month with comparison to previous month
     */
    public FinancialSummaryResponse getFinancialSummary(Long userId) {
        String currentMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        String previousMonth = YearMonth.now().minusMonths(1).format(YEAR_MONTH_FORMATTER);

        List<MonthlySummary> currentSummaries = monthlySummaryRepository.findByUserIdAndYearMonth(userId, currentMonth);
        List<MonthlySummary> previousSummaries = monthlySummaryRepository.findByUserIdAndYearMonth(userId, previousMonth);

        // Build current month summary by currency
        List<FinancialSummaryResponse.CurrencySummary> byCurrency = currentSummaries.stream()
                .map(ms -> FinancialSummaryResponse.CurrencySummary.builder()
                        .currency(ms.getCurrency())
                        .totalIncome(ms.getTotalIncome())
                        .totalExpense(ms.getTotalExpense())
                        .netChange(ms.getNetChange())
                        .savingsRate(ms.getSavingsRate())
                        .build())
                .collect(Collectors.toList());

        // Calculate totals in TWD
        BigDecimal totalIncomeInTwd = calculateTotalInTwd(currentSummaries, MonthlySummary::getTotalIncome);
        BigDecimal totalExpenseInTwd = calculateTotalInTwd(currentSummaries, MonthlySummary::getTotalExpense);
        BigDecimal netChangeInTwd = totalIncomeInTwd.subtract(totalExpenseInTwd);
        BigDecimal overallSavingsRate = totalIncomeInTwd.compareTo(BigDecimal.ZERO) > 0
                ? netChangeInTwd.divide(totalIncomeInTwd, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate comparison with previous month
        BigDecimal prevIncomeInTwd = calculateTotalInTwd(previousSummaries, MonthlySummary::getTotalIncome);
        BigDecimal prevExpenseInTwd = calculateTotalInTwd(previousSummaries, MonthlySummary::getTotalExpense);
        BigDecimal prevNetChange = prevIncomeInTwd.subtract(prevExpenseInTwd);
        BigDecimal prevSavingsRate = prevIncomeInTwd.compareTo(BigDecimal.ZERO) > 0
                ? prevNetChange.divide(prevIncomeInTwd, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal incomeChangePercent = calculateChangePercent(totalIncomeInTwd, prevIncomeInTwd);
        BigDecimal expenseChangePercent = calculateChangePercent(totalExpenseInTwd, prevExpenseInTwd);
        BigDecimal savingsRateChange = overallSavingsRate.subtract(prevSavingsRate);

        return FinancialSummaryResponse.builder()
                .currentMonth(FinancialSummaryResponse.CurrentMonthSummary.builder()
                        .byCurrency(byCurrency)
                        .totalIncomeInTwd(totalIncomeInTwd)
                        .totalExpenseInTwd(totalExpenseInTwd)
                        .netChangeInTwd(netChangeInTwd)
                        .overallSavingsRate(overallSavingsRate.setScale(2, RoundingMode.HALF_UP))
                        .build())
                .comparison(FinancialSummaryResponse.Comparison.builder()
                        .incomeChangePercent(incomeChangePercent)
                        .expenseChangePercent(expenseChangePercent)
                        .savingsRateChange(savingsRateChange.setScale(2, RoundingMode.HALF_UP))
                        .build())
                .build();
    }

    /**
     * Get income/expense data for the specified number of months
     */
    public IncomeExpenseResponse getIncomeExpense(Long userId, int months) {
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(months - 1);

        String startMonthStr = startMonth.format(YEAR_MONTH_FORMATTER);
        String endMonthStr = endMonth.format(YEAR_MONTH_FORMATTER);

        List<MonthlySummary> summaries = monthlySummaryRepository.findByUserIdAndMonthRange(userId, startMonthStr, endMonthStr);

        List<IncomeExpenseResponse.MonthlySummaryItem> monthlyItems = summaries.stream()
                .map(ms -> IncomeExpenseResponse.MonthlySummaryItem.builder()
                        .yearMonth(ms.getYearMonth())
                        .currency(ms.getCurrency())
                        .income(ms.getTotalIncome())
                        .expense(ms.getTotalExpense())
                        .netChange(ms.getNetChange())
                        .savingsRate(ms.getSavingsRate())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalIncome = calculateTotalInTwd(summaries, MonthlySummary::getTotalIncome);
        BigDecimal totalExpense = calculateTotalInTwd(summaries, MonthlySummary::getTotalExpense);
        BigDecimal netChange = totalIncome.subtract(totalExpense);
        BigDecimal averageSavingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netChange.divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return IncomeExpenseResponse.builder()
                .period(startMonthStr + " to " + endMonthStr)
                .monthly(monthlyItems)
                .total(IncomeExpenseResponse.TotalSummary.builder()
                        .totalIncome(totalIncome)
                        .totalExpense(totalExpense)
                        .netChange(netChange)
                        .averageSavingsRate(averageSavingsRate.setScale(2, RoundingMode.HALF_UP))
                        .build())
                .build();
    }

    /**
     * Get balance trends for the specified number of months
     */
    public TrendDataResponse getTrends(Long userId, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);

        List<BalanceSnapshot> snapshots = balanceSnapshotRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        // Group by date
        Map<LocalDate, List<BalanceSnapshot>> byDate = snapshots.stream()
                .collect(Collectors.groupingBy(BalanceSnapshot::getSnapshotDate));

        List<TrendDataResponse.TrendPoint> trends = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<BalanceSnapshot> daySnapshots = entry.getValue();

                    List<TrendDataResponse.CurrencyBalance> byCurrency = daySnapshots.stream()
                            .map(bs -> {
                                BigDecimal rate = EXCHANGE_RATES_TO_TWD.getOrDefault(bs.getCurrency(), BigDecimal.ONE);
                                return TrendDataResponse.CurrencyBalance.builder()
                                        .currency(bs.getCurrency())
                                        .balance(bs.getBalance())
                                        .balanceInTwd(bs.getBalance().multiply(rate))
                                        .build();
                            })
                            .collect(Collectors.toList());

                    BigDecimal twdBalance = byCurrency.stream()
                            .filter(cb -> "TWD".equals(cb.getCurrency()))
                            .map(TrendDataResponse.CurrencyBalance::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal foreignBalanceInTwd = byCurrency.stream()
                            .filter(cb -> !"TWD".equals(cb.getCurrency()))
                            .map(TrendDataResponse.CurrencyBalance::getBalanceInTwd)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalBalance = twdBalance.add(foreignBalanceInTwd);

                    return TrendDataResponse.TrendPoint.builder()
                            .date(date)
                            .totalBalance(totalBalance)
                            .twdBalance(twdBalance)
                            .foreignBalanceInTwd(foreignBalanceInTwd)
                            .byCurrency(byCurrency)
                            .build();
                })
                .collect(Collectors.toList());

        return TrendDataResponse.builder()
                .period(months + " months")
                .trends(trends)
                .build();
    }

    /**
     * Get current asset distribution by currency
     */
    public DistributionResponse getDistribution(Long userId) {
        // Get latest snapshots for each account
        LocalDate today = LocalDate.now();
        List<BalanceSnapshot> latestSnapshots = balanceSnapshotRepository.findByUserIdAndDate(userId, today);

        // If no data for today, try to get the most recent data
        if (latestSnapshots.isEmpty()) {
            latestSnapshots = balanceSnapshotRepository.findByUserIdAndDateRange(
                    userId,
                    today.minusDays(30),
                    today
            );
            // Group by account and get the latest for each
            Map<Long, BalanceSnapshot> latestByAccount = new HashMap<>();
            for (BalanceSnapshot bs : latestSnapshots) {
                latestByAccount.merge(bs.getAccountId(), bs, (existing, newOne) ->
                        newOne.getSnapshotDate().isAfter(existing.getSnapshotDate()) ? newOne : existing);
            }
            latestSnapshots = new ArrayList<>(latestByAccount.values());
        }

        // Group by currency and calculate totals
        Map<String, BigDecimal> balanceByCurrency = latestSnapshots.stream()
                .collect(Collectors.groupingBy(
                        BalanceSnapshot::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO, BalanceSnapshot::getBalance, BigDecimal::add)
                ));

        BigDecimal totalInTwd = balanceByCurrency.entrySet().stream()
                .map(entry -> {
                    BigDecimal rate = EXCHANGE_RATES_TO_TWD.getOrDefault(entry.getKey(), BigDecimal.ONE);
                    return entry.getValue().multiply(rate);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DistributionResponse.CurrencyDistribution> distribution = balanceByCurrency.entrySet().stream()
                .map(entry -> {
                    String currency = entry.getKey();
                    BigDecimal balance = entry.getValue();
                    BigDecimal rate = EXCHANGE_RATES_TO_TWD.getOrDefault(currency, BigDecimal.ONE);
                    BigDecimal balanceInTwd = balance.multiply(rate);
                    BigDecimal percentage = totalInTwd.compareTo(BigDecimal.ZERO) > 0
                            ? balanceInTwd.divide(totalInTwd, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return DistributionResponse.CurrencyDistribution.builder()
                            .currency(currency)
                            .balance(balance)
                            .balanceInTwd(balanceInTwd)
                            .percentage(percentage.setScale(2, RoundingMode.HALF_UP))
                            .build();
                })
                .sorted((a, b) -> b.getPercentage().compareTo(a.getPercentage()))
                .collect(Collectors.toList());

        return DistributionResponse.builder()
                .totalBalanceInTwd(totalInTwd)
                .distribution(distribution)
                .build();
    }

    // Helper methods
    private BigDecimal calculateTotalInTwd(List<MonthlySummary> summaries,
                                           java.util.function.Function<MonthlySummary, BigDecimal> extractor) {
        return summaries.stream()
                .map(ms -> {
                    BigDecimal value = extractor.apply(ms);
                    BigDecimal rate = EXCHANGE_RATES_TO_TWD.getOrDefault(ms.getCurrency(), BigDecimal.ONE);
                    return value != null ? value.multiply(rate) : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
