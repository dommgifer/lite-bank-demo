package com.litebank.exchangeservice.client;

import com.litebank.exchangeservice.exception.ExchangeException;
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
        try {
            String url = accountServiceUrl + "/api/v1/accounts/" + accountId;
            log.info("Getting account: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                return data;
            }

            throw new ExchangeException("ERR_EXC_001", "Failed to get account: " + accountId);

        } catch (Exception e) {
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_001", "Failed to get account: " + e.getMessage());
        }
    }

    public void updateAccountBalance(Long accountId, BigDecimal newBalance) {
        try {
            String url = accountServiceUrl + "/api/v1/accounts/" + accountId + "/balance?newBalance=" + newBalance;
            log.info("Updating account balance: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Account balance updated successfully: {}", accountId);
                return;
            }

            throw new ExchangeException("ERR_EXC_002", "Failed to update account balance");

        } catch (Exception e) {
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_002", "Failed to update account balance: " + e.getMessage());
        }
    }
}
