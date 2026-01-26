package com.litebank.transactionservice.service;

import com.litebank.transactionservice.dto.*;
import com.litebank.transactionservice.entity.Account;
import com.litebank.transactionservice.entity.Transaction;
import com.litebank.transactionservice.exception.AccountNotFoundException;
import com.litebank.transactionservice.exception.CurrencyMismatchException;
import com.litebank.transactionservice.exception.InsufficientBalanceException;
import com.litebank.transactionservice.exception.TransactionNotFoundException;
import com.litebank.transactionservice.repository.AccountRepository;
import com.litebank.transactionservice.repository.TransactionRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final Tracer tracer;
    private final TransactionEventPublisher eventPublisher;

    // ==================== 查詢方法 ====================

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long transactionId) {
        Span span = tracer.spanBuilder("TransactionService.getTransactionById")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("transaction.id", transactionId);

            return transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByAccount(Long accountId, int page, int size) {
        Span span = tracer.spanBuilder("TransactionService.getTransactionsByAccount")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);
            span.setAttribute("page", page);
            span.setAttribute("page", size);

            Pageable pageable = PageRequest.of(page, size);
            return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public Page<Transaction> queryTransactions(TransactionQueryParams params) {
        Span span = tracer.spanBuilder("TransactionService.queryTransactions")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            int page = params.getPage() != null ? params.getPage() : 0;
            int size = params.getSize() != null ? params.getSize() : 20;
            Pageable pageable = PageRequest.of(page, size);

            if (params.getAccountId() != null && params.getStartDate() != null && params.getEndDate() != null) {
                span.setAttribute("query.type", "date_range");
                return transactionRepository.findByAccountIdAndDateRange(
                        params.getAccountId(),
                        params.getStartDate(),
                        params.getEndDate(),
                        pageable
                );
            } else if (params.getAccountId() != null && params.getTransactionType() != null) {
                span.setAttribute("query.type", "account_and_type");
                Transaction.TransactionType type = Transaction.TransactionType.valueOf(params.getTransactionType());
                return transactionRepository.findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(
                        params.getAccountId(),
                        type,
                        pageable
                );
            } else if (params.getAccountId() != null) {
                span.setAttribute("query.type", "account_only");
                return transactionRepository.findByAccountIdOrderByCreatedAtDesc(params.getAccountId(), pageable);
            } else {
                span.setAttribute("query.type", "all");
                Pageable sortedPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                return transactionRepository.findAll(sortedPageable);
            }
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByTraceId(String traceId) {
        Span span = tracer.spanBuilder("TransactionService.getTransactionsByTraceId")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("trace.id", traceId);
            return transactionRepository.findByTraceId(traceId);
        } finally {
            span.end();
        }
    }

    // ==================== 舊版記錄交易方法（保持相容性）====================

    @Transactional
    public Transaction createTransaction(CreateTransactionRequest request, String traceId) {
        Span span = tracer.spanBuilder("TransactionService.createTransaction")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("transaction.type", request.getTransactionType());
            span.setAttribute("amount", request.getAmount().doubleValue());
            span.setAttribute("currency", request.getCurrency());

            Transaction transaction = Transaction.builder()
                    .accountId(request.getAccountId())
                    .transactionType(Transaction.TransactionType.valueOf(request.getTransactionType()))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .balanceAfter(request.getBalanceAfter())
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .metadata(request.getMetadata())
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);
            span.setAttribute("transaction.id", savedTransaction.getTransactionId());

            return savedTransaction;
        } finally {
            span.end();
        }
    }

    // ==================== 新增金流操作方法 ====================

    /**
     * 入款操作（DEPOSIT, TRANSFER_IN, EXCHANGE_IN）
     * 原子性操作：更新餘額 + 記錄交易
     */
    @Transactional
    public Transaction credit(CreditRequest request, String traceId) {
        Span span = tracer.spanBuilder("TransactionService.credit")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("transaction.type", request.getTransactionType());
            span.setAttribute("amount", request.getAmount().doubleValue());
            span.setAttribute("currency", request.getCurrency());

            log.info("Processing credit: accountId={}, type={}, amount={} {}",
                    request.getAccountId(), request.getTransactionType(),
                    request.getAmount(), request.getCurrency());

            // 使用悲觀鎖查詢帳戶
            Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Account not found: " + request.getAccountId()));

            // 驗證幣別
            if (!account.getCurrency().equals(request.getCurrency())) {
                throw new CurrencyMismatchException(
                        String.format("Currency mismatch. Account: %s, Request: %s",
                                account.getCurrency(), request.getCurrency()));
            }

            // 更新餘額
            BigDecimal oldBalance = account.getBalance();
            BigDecimal newBalance = oldBalance.add(request.getAmount());
            account.setBalance(newBalance);
            accountRepository.save(account);

            log.info("Account balance updated: accountId={}, {} -> {}",
                    request.getAccountId(), oldBalance, newBalance);

            // 記錄交易
            Transaction transaction = Transaction.builder()
                    .accountId(request.getAccountId())
                    .transactionType(Transaction.TransactionType.valueOf(request.getTransactionType()))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .balanceAfter(newBalance)
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);
            span.setAttribute("transaction.id", savedTransaction.getTransactionId());

            log.info("Credit transaction created: txnId={}", savedTransaction.getTransactionId());

            // Publish event to Kafka
            eventPublisher.publishTransactionCreated(savedTransaction, account.getUserId());

            return savedTransaction;
        } finally {
            span.end();
        }
    }

    /**
     * 出款操作（WITHDRAWAL, TRANSFER_OUT, EXCHANGE_OUT）
     * 原子性操作：驗證餘額 + 更新餘額 + 記錄交易
     */
    @Transactional
    public Transaction debit(DebitRequest request, String traceId) {
        Span span = tracer.spanBuilder("TransactionService.debit")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("transaction.type", request.getTransactionType());
            span.setAttribute("amount", request.getAmount().doubleValue());
            span.setAttribute("currency", request.getCurrency());

            log.info("Processing debit: accountId={}, type={}, amount={} {}",
                    request.getAccountId(), request.getTransactionType(),
                    request.getAmount(), request.getCurrency());

            // 使用悲觀鎖查詢帳戶
            Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Account not found: " + request.getAccountId()));

            // 驗證幣別
            if (!account.getCurrency().equals(request.getCurrency())) {
                throw new CurrencyMismatchException(
                        String.format("Currency mismatch. Account: %s, Request: %s",
                                account.getCurrency(), request.getCurrency()));
            }

            // 驗證餘額
            if (account.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance. Account: %s, Available: %s, Required: %s",
                                request.getAccountId(), account.getBalance(), request.getAmount()));
            }

            // 更新餘額
            BigDecimal oldBalance = account.getBalance();
            BigDecimal newBalance = oldBalance.subtract(request.getAmount());
            account.setBalance(newBalance);
            accountRepository.save(account);

            log.info("Account balance updated: accountId={}, {} -> {}",
                    request.getAccountId(), oldBalance, newBalance);

            // 記錄交易
            Transaction transaction = Transaction.builder()
                    .accountId(request.getAccountId())
                    .transactionType(Transaction.TransactionType.valueOf(request.getTransactionType()))
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .balanceAfter(newBalance)
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .build();

            Transaction savedTransaction = transactionRepository.save(transaction);
            span.setAttribute("transaction.id", savedTransaction.getTransactionId());

            log.info("Debit transaction created: txnId={}", savedTransaction.getTransactionId());

            // Publish event to Kafka
            eventPublisher.publishTransactionCreated(savedTransaction, account.getUserId());

            return savedTransaction;
        } finally {
            span.end();
        }
    }

    /**
     * 轉帳操作（同幣別）
     * 原子性操作：扣款 + 入帳 + 記錄兩筆交易
     */
    @Transactional
    public List<Transaction> transfer(TransferRequest request, String traceId) {
        Span span = tracer.spanBuilder("TransactionService.transfer")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("source.account.id", request.getSourceAccountId());
            span.setAttribute("destination.account.id", request.getDestinationAccountId());
            span.setAttribute("amount", request.getAmount().doubleValue());
            span.setAttribute("currency", request.getCurrency());

            log.info("Processing transfer: {} -> {}, amount={} {}",
                    request.getSourceAccountId(), request.getDestinationAccountId(),
                    request.getAmount(), request.getCurrency());

            // 先鎖定兩個帳戶（按 ID 順序鎖定，避免死鎖）
            Long firstId = Math.min(request.getSourceAccountId(), request.getDestinationAccountId());
            Long secondId = Math.max(request.getSourceAccountId(), request.getDestinationAccountId());

            Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstId));
            Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondId));

            Account sourceAccount = request.getSourceAccountId().equals(firstId) ? firstAccount : secondAccount;
            Account destAccount = request.getDestinationAccountId().equals(firstId) ? firstAccount : secondAccount;

            // 驗證幣別
            if (!sourceAccount.getCurrency().equals(request.getCurrency())) {
                throw new CurrencyMismatchException("Source account currency mismatch");
            }
            if (!destAccount.getCurrency().equals(request.getCurrency())) {
                throw new CurrencyMismatchException("Destination account currency mismatch");
            }

            // 驗證來源帳戶餘額
            if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance in source account. Available: %s, Required: %s",
                                sourceAccount.getBalance(), request.getAmount()));
            }

            // 更新來源帳戶餘額
            BigDecimal sourceOldBalance = sourceAccount.getBalance();
            BigDecimal sourceNewBalance = sourceOldBalance.subtract(request.getAmount());
            sourceAccount.setBalance(sourceNewBalance);
            accountRepository.save(sourceAccount);

            // 更新目標帳戶餘額
            BigDecimal destOldBalance = destAccount.getBalance();
            BigDecimal destNewBalance = destOldBalance.add(request.getAmount());
            destAccount.setBalance(destNewBalance);
            accountRepository.save(destAccount);

            log.info("Transfer balances updated: source {} -> {}, dest {} -> {}",
                    sourceOldBalance, sourceNewBalance, destOldBalance, destNewBalance);

            // 記錄出款交易
            Transaction outTransaction = Transaction.builder()
                    .accountId(request.getSourceAccountId())
                    .transactionType(Transaction.TransactionType.TRANSFER_OUT)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .balanceAfter(sourceNewBalance)
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .build();

            Transaction savedOutTransaction = transactionRepository.save(outTransaction);

            // 記錄入款交易
            Transaction inTransaction = Transaction.builder()
                    .accountId(request.getDestinationAccountId())
                    .transactionType(Transaction.TransactionType.TRANSFER_IN)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .balanceAfter(destNewBalance)
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .build();

            Transaction savedInTransaction = transactionRepository.save(inTransaction);

            log.info("Transfer transactions created: outTxn={}, inTxn={}",
                    savedOutTransaction.getTransactionId(), savedInTransaction.getTransactionId());

            // Publish events to Kafka
            eventPublisher.publishTransactionCreated(savedOutTransaction, sourceAccount.getUserId());
            eventPublisher.publishTransactionCreated(savedInTransaction, destAccount.getUserId());

            return List.of(savedOutTransaction, savedInTransaction);
        } finally {
            span.end();
        }
    }

    /**
     * 換匯操作（跨幣別）
     * 原子性操作：扣款 + 入帳 + 記錄兩筆交易
     */
    @Transactional
    public List<Transaction> exchange(ExchangeRequest request, String traceId) {
        Span span = tracer.spanBuilder("TransactionService.exchange")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("source.account.id", request.getSourceAccountId());
            span.setAttribute("destination.account.id", request.getDestinationAccountId());
            span.setAttribute("source.amount", request.getSourceAmount().doubleValue());
            span.setAttribute("destination.amount", request.getDestinationAmount().doubleValue());
            span.setAttribute("exchange.rate", request.getExchangeRate().doubleValue());

            log.info("Processing exchange: {} -> {}, {} {} -> {} {} (rate: {})",
                    request.getSourceAccountId(), request.getDestinationAccountId(),
                    request.getSourceAmount(), request.getSourceCurrency(),
                    request.getDestinationAmount(), request.getDestinationCurrency(),
                    request.getExchangeRate());

            // 先鎖定兩個帳戶（按 ID 順序鎖定，避免死鎖）
            Long firstId = Math.min(request.getSourceAccountId(), request.getDestinationAccountId());
            Long secondId = Math.max(request.getSourceAccountId(), request.getDestinationAccountId());

            Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstId));
            Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondId));

            Account sourceAccount = request.getSourceAccountId().equals(firstId) ? firstAccount : secondAccount;
            Account destAccount = request.getDestinationAccountId().equals(firstId) ? firstAccount : secondAccount;

            // 驗證幣別
            if (!sourceAccount.getCurrency().equals(request.getSourceCurrency())) {
                throw new CurrencyMismatchException("Source account currency mismatch");
            }
            if (!destAccount.getCurrency().equals(request.getDestinationCurrency())) {
                throw new CurrencyMismatchException("Destination account currency mismatch");
            }

            // 驗證來源帳戶餘額
            if (sourceAccount.getBalance().compareTo(request.getSourceAmount()) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance in source account. Available: %s, Required: %s",
                                sourceAccount.getBalance(), request.getSourceAmount()));
            }

            // 更新來源帳戶餘額
            BigDecimal sourceOldBalance = sourceAccount.getBalance();
            BigDecimal sourceNewBalance = sourceOldBalance.subtract(request.getSourceAmount());
            sourceAccount.setBalance(sourceNewBalance);
            accountRepository.save(sourceAccount);

            // 更新目標帳戶餘額
            BigDecimal destOldBalance = destAccount.getBalance();
            BigDecimal destNewBalance = destOldBalance.add(request.getDestinationAmount());
            destAccount.setBalance(destNewBalance);
            accountRepository.save(destAccount);

            log.info("Exchange balances updated: source {} {} -> {} {}, dest {} {} -> {} {}",
                    sourceOldBalance, request.getSourceCurrency(), sourceNewBalance, request.getSourceCurrency(),
                    destOldBalance, request.getDestinationCurrency(), destNewBalance, request.getDestinationCurrency());

            // 記錄出款交易
            Transaction outTransaction = Transaction.builder()
                    .accountId(request.getSourceAccountId())
                    .transactionType(Transaction.TransactionType.EXCHANGE_OUT)
                    .amount(request.getSourceAmount())
                    .currency(request.getSourceCurrency())
                    .balanceAfter(sourceNewBalance)
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .build();

            Transaction savedOutTransaction = transactionRepository.save(outTransaction);

            // 記錄入款交易
            Transaction inTransaction = Transaction.builder()
                    .accountId(request.getDestinationAccountId())
                    .transactionType(Transaction.TransactionType.EXCHANGE_IN)
                    .amount(request.getDestinationAmount())
                    .currency(request.getDestinationCurrency())
                    .balanceAfter(destNewBalance)
                    .referenceId(request.getReferenceId())
                    .description(request.getDescription())
                    .traceId(traceId)
                    .build();

            Transaction savedInTransaction = transactionRepository.save(inTransaction);

            log.info("Exchange transactions created: outTxn={}, inTxn={}",
                    savedOutTransaction.getTransactionId(), savedInTransaction.getTransactionId());

            // Publish events to Kafka
            eventPublisher.publishTransactionCreated(savedOutTransaction, sourceAccount.getUserId());
            eventPublisher.publishTransactionCreated(savedInTransaction, destAccount.getUserId());

            return List.of(savedOutTransaction, savedInTransaction);
        } finally {
            span.end();
        }
    }
}
