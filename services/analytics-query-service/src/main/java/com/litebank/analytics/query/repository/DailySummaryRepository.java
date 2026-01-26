package com.litebank.analytics.query.repository;

import com.litebank.analytics.query.model.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    @Query("SELECT ds FROM DailySummary ds WHERE ds.userId = :userId AND ds.summaryDate BETWEEN :startDate AND :endDate ORDER BY ds.summaryDate DESC")
    List<DailySummary> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT ds FROM DailySummary ds WHERE ds.userId = :userId AND ds.summaryDate = :date")
    List<DailySummary> findByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );
}
