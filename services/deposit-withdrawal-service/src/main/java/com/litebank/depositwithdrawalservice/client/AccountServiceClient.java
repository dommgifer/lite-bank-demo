package com.litebank.depositwithdrawalservice.client;

import com.litebank.depositwithdrawalservice.exception.DepositWithdrawalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.account-service.url}")
    private String accountServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(Long accountId) {
        String url = accountServiceUrl + "/api/v1/accounts/" + accountId;
        log.info("Getting account: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    Map.class
            );

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                return (Map<String, Object>) response.getBody().get("data");
            }

            throw new DepositWithdrawalException("ERR_DEP_001", "Failed to get account: " + accountId);
        } catch (Exception e) {
            if (e instanceof DepositWithdrawalException) throw e;
            throw new DepositWithdrawalException("ERR_DEP_001", "Failed to get account: " + e.getMessage());
        }
    }

    public void updateAccountBalance(Long accountId, BigDecimal newBalance) {
        String url = accountServiceUrl + "/api/v1/accounts/" + accountId + "/balance?newBalance=" + newBalance;
        log.info("Updating account balance: {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Account balance updated successfully: {}", accountId);
                return;
            }

            throw new DepositWithdrawalException("ERR_DEP_002", "Failed to update account balance");
        } catch (Exception e) {
            if (e instanceof DepositWithdrawalException) throw e;
            throw new DepositWithdrawalException("ERR_DEP_002", "Failed to update account balance: " + e.getMessage());
        }
    }
}
