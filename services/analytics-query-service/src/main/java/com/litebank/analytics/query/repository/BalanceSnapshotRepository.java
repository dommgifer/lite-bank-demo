package com.litebank.analytics.query.repository;

import com.litebank.analytics.query.model.entity.BalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, Long> {

    @Query("SELECT bs FROM BalanceSnapshot bs WHERE bs.userId = :userId AND bs.snapshotDate BETWEEN :startDate AND :endDate ORDER BY bs.snapshotDate ASC")
    List<BalanceSnapshot> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT bs FROM BalanceSnapshot bs WHERE bs.userId = :userId AND bs.snapshotDate = :date")
    List<BalanceSnapshot> findByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    @Query("SELECT bs FROM BalanceSnapshot bs WHERE bs.userId = :userId ORDER BY bs.snapshotDate DESC LIMIT 1")
    List<BalanceSnapshot> findLatestByUserId(@Param("userId") Long userId);
}
