package com.litebank.accountservice.service;

import com.litebank.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Generates structured account numbers in the format: {branch}-{type}-{sequence}
 * Format: XXX-YY-ZZZZZZZ (15 characters total)
 *
 * Branch codes:
 * - 001: Head Office
 *
 * Account types:
 * - 01: Savings (TWD - local currency)
 * - 03: Foreign Currency (USD, EUR, JPY, GBP, etc.)
 *
 * Sequence: 7-digit zero-padded number
 */
@Service
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final String DEFAULT_BRANCH = "001";
    private static final String TYPE_SAVINGS = "01";
    private static final String TYPE_FOREIGN = "03";
    private static final String LOCAL_CURRENCY = "TWD";

    private final AccountRepository accountRepository;

    /**
     * Generates a new account number based on currency type.
     *
     * @param currency The currency code (e.g., "TWD", "USD", "EUR")
     * @return A formatted account number like "001-01-0000001"
     */
    public String generate(String currency) {
        String accountType = getAccountType(currency);
        long nextSequence = getNextSequence();

        return String.format("%s-%s-%07d", DEFAULT_BRANCH, accountType, nextSequence);
    }

    private String getAccountType(String currency) {
        return LOCAL_CURRENCY.equals(currency) ? TYPE_SAVINGS : TYPE_FOREIGN;
    }

    private long getNextSequence() {
        // Use count + 1 as sequence to ensure uniqueness
        return accountRepository.count() + 1;
    }
}
