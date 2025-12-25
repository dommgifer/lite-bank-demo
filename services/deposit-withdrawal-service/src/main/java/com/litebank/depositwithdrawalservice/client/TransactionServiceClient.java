package com.litebank.depositwithdrawalservice.client;

import com.litebank.depositwithdrawalservice.dto.ApiResponse;
import com.litebank.depositwithdrawalservice.dto.CreditRequest;
import com.litebank.depositwithdrawalservice.dto.DebitRequest;
import com.litebank.depositwithdrawalservice.dto.TransactionResponse;
import com.litebank.depositwithdrawalservice.exception.DepositWithdrawalException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
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
    private final Tracer tracer;

    @Value("${services.transaction-service.url}")
    private String transactionServiceUrl;

    /**
     * Credit (deposit) operation - atomically updates balance and creates transaction
     */
    public TransactionResponse credit(CreditRequest request) {
        Span span = tracer.spanBuilder("TransactionServiceClient.credit")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("amount", request.getAmount().toString());

            String url = transactionServiceUrl + "/api/v1/transactions/credit";
            log.debug("Calling Transaction Service: POST {}", url);

            HttpHeaders headers = createTracingHeaders(span);
            HttpEntity<CreditRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<TransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<TransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                TransactionResponse txn = response.getBody().getData();
                span.setAttribute("transaction.id", txn.getTransactionId());
                span.setStatus(StatusCode.OK);
                return txn;
            }

            throw new DepositWithdrawalException("ERR_DEP_003", "Failed to credit account");
        } catch (Exception e) {
            span.recordException(e);
            log.error("Error crediting account: {}", e.getMessage());
            if (e instanceof DepositWithdrawalException) throw e;
            throw new DepositWithdrawalException("ERR_DEP_003", "Failed to communicate with Transaction Service: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    /**
     * Debit (withdrawal) operation - atomically updates balance and creates transaction
     */
    public TransactionResponse debit(DebitRequest request) {
        Span span = tracer.spanBuilder("TransactionServiceClient.debit")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", request.getAccountId());
            span.setAttribute("amount", request.getAmount().toString());

            String url = transactionServiceUrl + "/api/v1/transactions/debit";
            log.debug("Calling Transaction Service: POST {}", url);

            HttpHeaders headers = createTracingHeaders(span);
            HttpEntity<DebitRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ApiResponse<TransactionResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<TransactionResponse>>() {}
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                TransactionResponse txn = response.getBody().getData();
                span.setAttribute("transaction.id", txn.getTransactionId());
                span.setStatus(StatusCode.OK);
                return txn;
            }

            throw new DepositWithdrawalException("ERR_WTH_004", "Failed to debit account");
        } catch (Exception e) {
            span.recordException(e);
            log.error("Error debiting account: {}", e.getMessage());
            if (e instanceof DepositWithdrawalException) throw e;
            throw new DepositWithdrawalException("ERR_WTH_004", "Failed to communicate with Transaction Service: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    @SuppressWarnings("unchecked")
    public Long createTransaction(Long accountId, String transactionType, BigDecimal amount,
                                  String currency, BigDecimal balanceAfter, String referenceId,
                                  String description, String traceId) {

        Span span = tracer.spanBuilder("TransactionServiceClient.createTransaction")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("account.id", accountId);
            span.setAttribute("transaction.type", transactionType);
            span.setAttribute("amount", amount.toString());
            span.setAttribute("currency", currency);

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

            HttpHeaders headers = createTracingHeaders(span);
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

                span.setAttribute("transaction.id", transactionId);
                span.setStatus(StatusCode.OK);

                log.info("Created transaction: {}", transactionId);
                return transactionId;
            }

            span.setStatus(StatusCode.ERROR, "Failed to create transaction");
            throw new DepositWithdrawalException("ERR_DEP_003", "Failed to create transaction");

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            if (e instanceof DepositWithdrawalException) throw e;
            throw new DepositWithdrawalException("ERR_DEP_003", "Failed to create transaction: " + e.getMessage());
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
