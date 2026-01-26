package com.litebank.analytics.query.repository;

import com.litebank.analytics.query.model.entity.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, Long> {

    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId AND ms.yearMonth = :yearMonth")
    List<MonthlySummary> findByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth
    );

    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId AND ms.yearMonth >= :startMonth AND ms.yearMonth <= :endMonth ORDER BY ms.yearMonth DESC")
    List<MonthlySummary> findByUserIdAndMonthRange(
            @Param("userId") Long userId,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth
    );

    @Query("SELECT ms FROM MonthlySummary ms WHERE ms.userId = :userId ORDER BY ms.yearMonth DESC")
    List<MonthlySummary> findByUserIdOrderByYearMonthDesc(@Param("userId") Long userId);
}
