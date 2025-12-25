package com.litebank.transferservice.service;

import com.litebank.transferservice.dto.AccountResponse;
import com.litebank.transferservice.dto.ApiResponse;
import com.litebank.transferservice.exception.ServiceCommunicationException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
    private final Tracer tracer;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    public AccountResponse getAccount(Long accountId) {
        Span span = tracer.spanBuilder("AccountServiceClient.getAccount").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);

            String url = accountServiceUrl + "/api/v1/accounts/" + accountId;
            log.debug("Calling Account Service: GET {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("traceparent", String.format("00-%s-%s-01",
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId()));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

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
            span.recordException(e);
            log.error("Error calling Account Service for account {}: {}", accountId, e.getMessage());
            throw new ServiceCommunicationException("Failed to communicate with Account Service: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    public AccountResponse updateAccountBalance(Long accountId, BigDecimal newBalance) {
        Span span = tracer.spanBuilder("AccountServiceClient.updateAccountBalance").startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);
            span.setAttribute("new.balance", newBalance.toString());

            String url = accountServiceUrl + "/api/v1/accounts/" + accountId + "/balance?newBalance=" + newBalance;
            log.debug("Calling Account Service: PUT {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("traceparent", String.format("00-%s-%s-01",
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId()));

            HttpEntity<Void> entity = new HttpEntity<>(headers);

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
            span.recordException(e);
            log.error("Error updating account balance for account {}: {}", accountId, e.getMessage());
            throw new ServiceCommunicationException("Failed to update account balance: " + e.getMessage());
        } finally {
            span.end();
        }
    }
}
