package com.litebank.exchangeservice.client;

import com.litebank.exchangeservice.exception.ExchangeException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
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
    private final Tracer tracer;

    @Value("${services.account-service.url}")
    private String accountServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(Long accountId) {
        Span span = tracer.spanBuilder("AccountServiceClient.getAccount")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);

            String url = accountServiceUrl + "/api/v1/accounts/" + accountId;
            log.info("Getting account: {}", url);

            HttpHeaders headers = createTracingHeaders(span);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                span.setStatus(StatusCode.OK);
                return data;
            }

            span.setStatus(StatusCode.ERROR, "Failed to get account");
            throw new ExchangeException("ERR_EXC_001", "Failed to get account: " + accountId);

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_001", "Failed to get account: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    public void updateAccountBalance(Long accountId, BigDecimal newBalance) {
        Span span = tracer.spanBuilder("AccountServiceClient.updateAccountBalance")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);
            span.setAttribute("new.balance", newBalance.toString());

            String url = accountServiceUrl + "/api/v1/accounts/" + accountId + "/balance?newBalance=" + newBalance;
            log.info("Updating account balance: {}", url);

            HttpHeaders headers = createTracingHeaders(span);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                span.setStatus(StatusCode.OK);
                log.info("Account balance updated successfully: {}", accountId);
                return;
            }

            span.setStatus(StatusCode.ERROR, "Failed to update balance");
            throw new ExchangeException("ERR_EXC_002", "Failed to update account balance");

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_002", "Failed to update account balance: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    private HttpHeaders createTracingHeaders(Span span) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String traceId = span.getSpanContext().getTraceId();
        String spanId = span.getSpanContext().getSpanId();
        String traceFlags = span.getSpanContext().getTraceFlags().asHex();
        String traceparent = String.format("00-%s-%s-%s", traceId, spanId, traceFlags);
        headers.set("traceparent", traceparent);

        return headers;
    }
}
