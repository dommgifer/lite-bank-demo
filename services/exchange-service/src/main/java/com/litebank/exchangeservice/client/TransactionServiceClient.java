package com.litebank.exchangeservice.client;

import com.litebank.exchangeservice.dto.ApiResponse;
import com.litebank.exchangeservice.dto.ExchangeTransactionRequest;
import com.litebank.exchangeservice.dto.ExchangeTransactionResponse;
import com.litebank.exchangeservice.exception.ExchangeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.transaction-service.url}")
    private String transactionServiceUrl;

    /**
     * Exchange operation - atomically updates both account balances and creates transactions
     */
    public ExchangeTransactionResponse exchange(ExchangeTransactionRequest request) {
        try {
            String url = transactionServiceUrl + "/api/v1/transactions/exchange";
            log.debug("Calling Transaction Service: POST {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ExchangeTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<ExchangeTransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<ExchangeTransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                ExchangeTransactionResponse result = response.getBody().getData();
                return result;
            }

            throw new ExchangeException("ERR_EXC_012", "Failed to execute exchange");
        } catch (Exception e) {
            log.error("Error executing exchange: {}", e.getMessage());
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_012", "Failed to communicate with Transaction Service: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Long createTransaction(Long accountId, String transactionType, BigDecimal amount,
                                  String currency, BigDecimal balanceAfter, String referenceId,
                                  String description, String traceId) {

        try {
            String url = transactionServiceUrl + "/api/v1/transactions";
            log.info("Creating transaction: {}", url);

            Map<String, Object> request = new HashMap<>();
            request.put("accountId", accountId);
            request.put("transactionType", transactionType);
            request.put("amount", amount);
            request.put("currency", currency);
            request.put("balanceAfter", balanceAfter);
            request.put("referenceId", referenceId);
            request.put("description", description);
            request.put("traceId", traceId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                Long transactionId = ((Number) data.get("transactionId")).longValue();

                log.info("Created transaction: {}", transactionId);
                return transactionId;
            }

            throw new ExchangeException("ERR_EXC_005", "Failed to create transaction");

        } catch (Exception e) {
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_005", "Failed to create transaction: " + e.getMessage());
        }
    }
}
