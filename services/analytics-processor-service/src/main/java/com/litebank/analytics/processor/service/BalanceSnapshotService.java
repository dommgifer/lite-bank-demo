package com.litebank.analytics.processor.service;

import com.litebank.analytics.processor.model.entity.BalanceSnapshot;
import com.litebank.analytics.processor.model.event.TransactionCreatedEvent;
import com.litebank.analytics.processor.repository.BalanceSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceSnapshotService {

    private final BalanceSnapshotRepository balanceSnapshotRepository;

    @Transactional
    public void updateBalanceSnapshot(TransactionCreatedEvent event) {
        TransactionCreatedEvent.TransactionPayload payload = event.getPayload();
        LocalDate snapshotDate = payload.getCreatedAt().toLocalDate();

        // Find or create snapshot for this account and date
        BalanceSnapshot snapshot = balanceSnapshotRepository
                .findByAccountIdAndDate(payload.getAccountId(), snapshotDate)
                .orElseGet(() -> BalanceSnapshot.builder()
                        .userId(payload.getUserId())
                        .accountId(payload.getAccountId())
                        .currency(payload.getCurrency())
                        .snapshotDate(snapshotDate)
                        .build());

        // Update to the latest balance for the day
        snapshot.setBalance(payload.getBalanceAfter());

        balanceSnapshotRepository.save(snapshot);

        log.info("Balance snapshot updated: userId={}, accountId={}, date={}, currency={}, balance={}",
                payload.getUserId(),
                payload.getAccountId(),
                snapshotDate,
                payload.getCurrency(),
                snapshot.getBalance());
    }
}
