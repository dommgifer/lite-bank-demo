package com.litebank.analytics.processor.repository;

import com.litebank.analytics.processor.model.entity.BalanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BalanceSnapshotRepository extends JpaRepository<BalanceSnapshot, Long> {

    @Query("SELECT bs FROM BalanceSnapshot bs WHERE bs.accountId = :accountId AND bs.snapshotDate = :date")
    Optional<BalanceSnapshot> findByAccountIdAndDate(
            @Param("accountId") Long accountId,
            @Param("date") LocalDate date
    );
}
