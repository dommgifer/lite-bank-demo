package com.litebank.analytics.query.controller;

import com.litebank.analytics.query.dto.*;
import com.litebank.analytics.query.service.AnalyticsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Financial analytics and reporting APIs")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping("/summary")
    @Operation(summary = "Get financial summary", description = "Get current month financial summary with comparison to previous month")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> getSummary(
            @Parameter(description = "User ID from JWT token", required = true)
            @RequestHeader("X-User-ID") Long userId
    ) {
        log.info("Getting financial summary for userId={}", userId);
        FinancialSummaryResponse summary = analyticsQueryService.getFinancialSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/income-expense")
    @Operation(summary = "Get income and expense data", description = "Get income and expense data for the specified period")
    public ResponseEntity<ApiResponse<IncomeExpenseResponse>> getIncomeExpense(
            @Parameter(description = "User ID from JWT token", required = true)
            @RequestHeader("X-User-ID") Long userId,
            @Parameter(description = "Number of months to include (default: 6)")
            @RequestParam(defaultValue = "6") int months
    ) {
        log.info("Getting income/expense data for userId={}, months={}", userId, months);
        IncomeExpenseResponse response = analyticsQueryService.getIncomeExpense(userId, months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get balance trends", description = "Get balance trends for the specified period")
    public ResponseEntity<ApiResponse<TrendDataResponse>> getTrends(
            @Parameter(description = "User ID from JWT token", required = true)
            @RequestHeader("X-User-ID") Long userId,
            @Parameter(description = "Number of months to include (default: 6)")
            @RequestParam(defaultValue = "6") int months
    ) {
        log.info("Getting balance trends for userId={}, months={}", userId, months);
        TrendDataResponse response = analyticsQueryService.getTrends(userId, months);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/distribution")
    @Operation(summary = "Get asset distribution", description = "Get current asset distribution by currency")
    public ResponseEntity<ApiResponse<DistributionResponse>> getDistribution(
            @Parameter(description = "User ID from JWT token", required = true)
            @RequestHeader("X-User-ID") Long userId
    ) {
        log.info("Getting asset distribution for userId={}", userId);
        DistributionResponse response = analyticsQueryService.getDistribution(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
