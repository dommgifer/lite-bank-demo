package com.litebank.analytics.processor.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_summary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;  // Format: '2024-01'

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_income", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expense", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "net_change", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal netChange = BigDecimal.ZERO;

    @Column(name = "savings_rate", precision = 5, scale = 2)
    private BigDecimal savingsRate;  // Percentage

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate net change and savings rate
     */
    public void calculateDerivedFields() {
        this.netChange = this.totalIncome.subtract(this.totalExpense);
        if (this.totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            this.savingsRate = this.netChange
                    .divide(this.totalIncome, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.savingsRate = BigDecimal.ZERO;
        }
    }
}
