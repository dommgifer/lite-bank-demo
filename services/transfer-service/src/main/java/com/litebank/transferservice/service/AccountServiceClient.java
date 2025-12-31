package com.litebank.transferservice.service;

import com.litebank.transferservice.dto.AccountResponse;
import com.litebank.transferservice.dto.ApiResponse;
import com.litebank.transferservice.exception.ServiceCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    public AccountResponse getAccount(Long accountId) {
        try {
            String url = accountServiceUrl + "/api/v1/accounts/" + accountId;
            log.debug("Calling Account Service: GET {}", url);

            HttpEntity<Void> entity = new HttpEntity<>(null);

            ResponseEntity<ApiResponse<AccountResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }

            throw new ServiceCommunicationException("Failed to get account from Account Service");
        } catch (Exception e) {
            log.error("Error calling Account Service for account {}: {}", accountId, e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with Account Service: " + e.getMessage());
        }
    }

    public AccountResponse updateAccountBalance(Long accountId, BigDecimal newBalance) {
        try {
            String url = accountServiceUrl + "/api/v1/accounts/" + accountId + "/balance?newBalance=" + newBalance;
            log.debug("Calling Account Service: PUT {}", url);

            HttpEntity<Void> entity = new HttpEntity<>(null);

            ResponseEntity<ApiResponse<AccountResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<AccountResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }

            throw new ServiceCommunicationException("Failed to update account balance");
        } catch (Exception e) {
            log.error("Error updating account balance for account {}: {}", accountId, e.getMessage());
            throw new ServiceCommunicationException("Failed to update account balance: " + e.getMessage());
        }
    }
}
