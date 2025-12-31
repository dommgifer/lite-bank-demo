package com.litebank.transferservice.service;

import com.litebank.transferservice.dto.ApiResponse;
import com.litebank.transferservice.dto.CreateTransactionRequest;
import com.litebank.transferservice.dto.TransactionResponse;
import com.litebank.transferservice.dto.TransferTransactionRequest;
import com.litebank.transferservice.dto.TransferTransactionResponse;
import com.litebank.transferservice.exception.ServiceCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${transaction.service.url}")
    private String transactionServiceUrl;

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        try {
            String url = transactionServiceUrl + "/api/v1/transactions";
            log.debug("Calling Transaction Service: POST {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CreateTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<TransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<TransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }

            throw new ServiceCommunicationException("Failed to create transaction");
        } catch (Exception e) {
            log.error("Error creating transaction: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with Transaction Service: " + e.getMessage());
        }
    }

    /**
     * Call Transaction Service's transfer API - atomic operation for transferring funds
     */
    public TransferTransactionResponse transfer(TransferTransactionRequest request) {
        try {
            String url = transactionServiceUrl + "/api/v1/transactions/transfer";
            log.debug("Calling Transaction Service: POST {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<TransferTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<TransferTransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<TransferTransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }

            throw new ServiceCommunicationException("Failed to execute transfer");
        } catch (Exception e) {
            log.error("Error executing transfer: {}", e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with Transaction Service: " + e.getMessage());
        }
    }
}
