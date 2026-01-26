package com.litebank.analytics.processor.repository;

import com.litebank.analytics.processor.model.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    @Query("SELECT ds FROM DailySummary ds WHERE ds.userId = :userId AND ds.summaryDate = :date AND ds.currency = :currency")
    Optional<DailySummary> findByUserIdAndDateAndCurrency(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("currency") String currency
    );
}
