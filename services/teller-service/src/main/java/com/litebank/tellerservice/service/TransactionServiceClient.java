package com.litebank.tellerservice.service;

import com.litebank.tellerservice.dto.*;
import com.litebank.tellerservice.exception.ServiceCommunicationException;
import com.litebank.tellerservice.exception.TellerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
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

    public TransactionResponse credit(CreditRequest request) {
        String url = transactionServiceUrl + "/api/v1/transactions/credit";
        log.debug("Calling Transaction Service: POST {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreditRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ApiResponse<TransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<TransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }

            throw new TellerException("ERR_DEP_003", "Failed to credit account");
        } catch (Exception e) {
            log.error("Error crediting account: {}", e.getMessage());
            if (e instanceof TellerException) throw e;
            throw new TellerException("ERR_DEP_003", "Failed to communicate with Transaction Service: " + e.getMessage());
        }
    }

    public TransactionResponse debit(DebitRequest request) {
        String url = transactionServiceUrl + "/api/v1/transactions/debit";
        log.debug("Calling Transaction Service: POST {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DebitRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ApiResponse<TransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<TransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                return response.getBody().getData();
            }

            throw new TellerException("ERR_WTH_004", "Failed to debit account");
        } catch (Exception e) {
            log.error("Error debiting account: {}", e.getMessage());
            if (e instanceof TellerException) throw e;
            throw new TellerException("ERR_WTH_004", "Failed to communicate with Transaction Service: " + e.getMessage());
        }
    }
}
