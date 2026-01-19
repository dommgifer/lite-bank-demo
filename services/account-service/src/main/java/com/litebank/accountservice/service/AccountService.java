package com.litebank.accountservice.service;

import com.litebank.accountservice.dto.AccountPublicInfoResponse;
import com.litebank.accountservice.dto.BalanceResponse;
import com.litebank.accountservice.dto.CreateAccountRequest;
import com.litebank.accountservice.entity.Account;
import com.litebank.accountservice.exception.AccountNotFoundException;
import com.litebank.accountservice.exception.DuplicateAccountException;
import com.litebank.accountservice.repository.AccountRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final Tracer tracer;

    @Transactional(readOnly = true)
    public Account getAccountById(Long accountId) {
        Span span = tracer.spanBuilder("AccountService.getAccountById")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);

            log.debug("Fetching account with ID: {}", accountId);

            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> {
                        span.setStatus(StatusCode.ERROR, "Account not found");
                        return new AccountNotFoundException("Account not found: " + accountId);
                    });

            span.setAttribute("account.currency", account.getCurrency());
            span.setAttribute("account.status", account.getStatus().name());
            span.setStatus(StatusCode.OK);

            return account;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUserId(Long userId) {
        Span span = tracer.spanBuilder("AccountService.getAccountsByUserId")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("user.id", userId);

            log.debug("Fetching accounts for user ID: {}", userId);

            List<Account> accounts = accountRepository.findByUserId(userId);

            span.setAttribute("accounts.count", accounts.size());
            span.setStatus(StatusCode.OK);

            return accounts;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public Account getAccountByAccountNumber(String accountNumber) {
        Span span = tracer.spanBuilder("AccountService.getAccountByAccountNumber")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.number", accountNumber);

            log.debug("Fetching account with number: {}", accountNumber);

            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> {
                        span.setStatus(StatusCode.ERROR, "Account not found");
                        return new AccountNotFoundException("Account not found: " + accountNumber);
                    });

            span.setAttribute("account.id", account.getAccountId());
            span.setAttribute("account.currency", account.getCurrency());
            span.setAttribute("account.status", account.getStatus().name());
            span.setStatus(StatusCode.OK);

            return account;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public AccountPublicInfoResponse getPublicAccountInfo(String accountNumber) {
        Span span = tracer.spanBuilder("AccountService.getPublicAccountInfo")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.number", accountNumber);

            log.debug("Fetching public info for account: {}", accountNumber);

            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> {
                        span.setStatus(StatusCode.ERROR, "Account not found");
                        return new AccountNotFoundException("Account not found: " + accountNumber);
                    });

            // Build public info response without sensitive data (balance)
            AccountPublicInfoResponse response = AccountPublicInfoResponse.builder()
                    .accountId(account.getAccountId())
                    .accountNumber(account.getAccountNumber())
                    .currency(account.getCurrency())
                    .status(account.getStatus().name())
                    .fullName(null)  // TODO: Implement user name lookup when full_name field is added
                    .build();

            span.setAttribute("account.currency", account.getCurrency());
            span.setAttribute("account.status", account.getStatus().name());
            span.setStatus(StatusCode.OK);

            return response;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public BalanceResponse getAccountBalance(Long accountId) {
        Span span = tracer.spanBuilder("AccountService.getAccountBalance")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);

            log.debug("Fetching balance for account ID: {}", accountId);

            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> {
                        span.setStatus(StatusCode.ERROR, "Account not found");
                        return new AccountNotFoundException("Account not found: " + accountId);
                    });

            BalanceResponse response = BalanceResponse.builder()
                    .accountId(account.getAccountId())
                    .currency(account.getCurrency())
                    .balance(account.getBalance())
                    .status(account.getStatus().name())
                    .build();

            span.setAttribute("balance.amount", account.getBalance().toString());
            span.setAttribute("balance.currency", account.getCurrency());
            span.setStatus(StatusCode.OK);

            return response;
        } finally {
            span.end();
        }
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        Span span = tracer.spanBuilder("AccountService.createAccount")
                .setParent(io.opentelemetry.context.Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("user.id", request.getUserId());
            span.setAttribute("account.currency", request.getCurrency());

            log.info("Creating new account for user ID: {}, currency: {}",
                    request.getUserId(), request.getCurrency());

            // Check if account already exists for this user and currency
            accountRepository.findByUserIdAndCurrency(request.getUserId(), request.getCurrency())
                    .ifPresent(existing -> {
                        String message = String.format("User %d already has a %s account",
                                request.getUserId(), request.getCurrency());
                        span.setStatus(StatusCode.ERROR, message);
                        throw new DuplicateAccountException(message);
                    });

            // Generate structured account number
            String accountNumber = accountNumberGenerator.generate(request.getCurrency());

            Account account = Account.builder()
                    .userId(request.getUserId())
                    .accountNumber(accountNumber)
                    .currency(request.getCurrency())
                    .balance(BigDecimal.ZERO)
                    .status(Account.AccountStatus.ACTIVE)
                    .build();

            Account savedAccount = accountRepository.save(account);

            span.setAttribute("account.id", savedAccount.getAccountId());
            span.setAttribute("account.number", savedAccount.getAccountNumber());
            span.setStatus(StatusCode.OK);

            log.info("Account created successfully: ID={}, Number={}, User={}, Currency={}",
                    savedAccount.getAccountId(), savedAccount.getAccountNumber(),
                    savedAccount.getUserId(), savedAccount.getCurrency());

            return savedAccount;
        } catch (DuplicateAccountException e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    // updateAccountBalance method removed - balance updates are now exclusively handled by Transaction Service
}
