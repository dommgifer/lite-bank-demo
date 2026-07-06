package com.litebank.transactionservice.repository;

import com.litebank.transactionservice.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 列表查詢一律回 Slice：Spring Data 對 Slice 不執行 SELECT COUNT(*)，
    // 避免在膨脹的 append-only ledger 上做全表/大範圍計數拖垮連線池（見 fix/txn-pool-exhaustion 止血後根治）。
    Slice<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    Slice<Transaction> findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(
            Long accountId, Transaction.TransactionType transactionType, Pageable pageable);

    // 全表列表：用 derived query 內建排序取代 JpaRepository.findAll(Pageable)（後者強制 count）
    Slice<Transaction> findByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
           "AND t.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    Slice<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.traceId = :traceId ORDER BY t.createdAt DESC")
    List<Transaction> findByTraceId(@Param("traceId") String traceId);
}
