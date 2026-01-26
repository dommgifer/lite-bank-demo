package com.litebank.analytics.query.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_income", precision = 18, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expense", precision = 18, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "ending_balance", precision = 18, scale = 2)
    private BigDecimal endingBalance;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
