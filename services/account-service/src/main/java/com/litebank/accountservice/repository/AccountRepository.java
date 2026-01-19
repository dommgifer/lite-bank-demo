package com.litebank.accountservice.repository;

import com.litebank.accountservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    List<Account> findByUserIdAndStatus(Long userId, Account.AccountStatus status);

    Optional<Account> findByUserIdAndCurrency(Long userId, String currency);

    Optional<Account> findByAccountNumber(String accountNumber);
}
