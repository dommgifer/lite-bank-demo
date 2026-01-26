package com.litebank.analytics.processor.repository;

import com.litebank.analytics.processor.model.entity.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, Long> {

    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId AND ms.yearMonth = :yearMonth AND ms.currency = :currency")
    Optional<MonthlySummary> findByUserIdAndYearMonthAndCurrency(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth,
            @Param("currency") String currency
    );
}
