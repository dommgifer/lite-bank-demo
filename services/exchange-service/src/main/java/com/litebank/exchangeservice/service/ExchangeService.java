package com.litebank.exchangeservice.service;

import com.litebank.exchangeservice.client.AccountServiceClient;
import com.litebank.exchangeservice.client.ExchangeRateServiceClient;
import com.litebank.exchangeservice.client.TransactionServiceClient;
import com.litebank.exchangeservice.dto.ExchangeRequest;
import com.litebank.exchangeservice.dto.ExchangeResponse;
import com.litebank.exchangeservice.dto.ExchangeTransactionRequest;
import com.litebank.exchangeservice.dto.ExchangeTransactionResponse;
import com.litebank.exchangeservice.exception.ExchangeException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final AccountServiceClient accountServiceClient;
    private final ExchangeRateServiceClient exchangeRateServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final Tracer tracer;

    public ExchangeResponse executeExchange(ExchangeRequest request) {
        Span span = tracer.spanBuilder("ExchangeService.executeExchange")
                .startSpan();

        String exchangeId = UUID.randomUUID().toString();
        String traceId = span.getSpanContext().getTraceId();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("exchange.id", exchangeId);
            span.setAttribute("source.account.id", request.getSourceAccountId());
            span.setAttribute("destination.account.id", request.getDestinationAccountId());
            span.setAttribute("source.currency", request.getSourceCurrency());
            span.setAttribute("destination.currency", request.getDestinationCurrency());
            span.setAttribute("amount", request.getAmount().toString());

            log.info("Starting exchange: {} - {} {} from account {} to account {} ({})",
                    exchangeId, request.getAmount(), request.getSourceCurrency(),
                    request.getSourceAccountId(), request.getDestinationAccountId(),
                    request.getDestinationCurrency());

            // Step 1: Get source account and validate currency
            Map<String, Object> sourceAccount = accountServiceClient.getAccount(request.getSourceAccountId());
            String sourceAccountCurrency = (String) sourceAccount.get("currency");

            // Validate source account currency
            if (!sourceAccountCurrency.equalsIgnoreCase(request.getSourceCurrency())) {
                throw new ExchangeException("ERR_EXC_010",
                        "Source account currency mismatch. Expected: " + request.getSourceCurrency()
                                + ", Actual: " + sourceAccountCurrency);
            }

            // Step 2: Get destination account and validate currency
            Map<String, Object> destAccount = accountServiceClient.getAccount(request.getDestinationAccountId());
            String destAccountCurrency = (String) destAccount.get("currency");

            // Validate destination account currency
            if (!destAccountCurrency.equalsIgnoreCase(request.getDestinationCurrency())) {
                throw new ExchangeException("ERR_EXC_011",
                        "Destination account currency mismatch. Expected: " + request.getDestinationCurrency()
                                + ", Actual: " + destAccountCurrency);
            }

            // Step 3: Get exchange rate
            log.info("Getting exchange rate from {} to {}", request.getSourceCurrency(), request.getDestinationCurrency());
            BigDecimal exchangeRate = exchangeRateServiceClient.getExchangeRate(
                    request.getSourceCurrency(),
                    request.getDestinationCurrency()
            );
            log.info("Exchange rate: {}", exchangeRate);

            // Step 4: Calculate destination amount
            BigDecimal destinationAmount = request.getAmount()
                    .multiply(exchangeRate)
                    .setScale(2, RoundingMode.HALF_UP);
            log.info("Destination amount: {}", destinationAmount);

            // Step 5: Execute atomic exchange operation via Transaction Service
            log.info("Executing atomic exchange operation via Transaction Service");

            String description = request.getDescription() != null ? request.getDescription()
                    : String.format("Currency exchange from %s to %s",
                            request.getSourceCurrency(), request.getDestinationCurrency());

            ExchangeTransactionRequest exchangeRequest = ExchangeTransactionRequest.builder()
                    .sourceAccountId(request.getSourceAccountId())
                    .destinationAccountId(request.getDestinationAccountId())
                    .sourceAmount(request.getAmount())
                    .sourceCurrency(request.getSourceCurrency())
                    .destinationAmount(destinationAmount)
                    .destinationCurrency(request.getDestinationCurrency())
                    .exchangeRate(exchangeRate)
                    .referenceId(exchangeId)
                    .description(description)
                    .build();

            ExchangeTransactionResponse exchangeResult = transactionServiceClient.exchange(exchangeRequest);

            log.info("Exchange completed - Source Txn: {}, Dest Txn: {}, Rate: {}, Dest Amount: {}",
                    exchangeResult.getSourceTransactionId(),
                    exchangeResult.getDestinationTransactionId(),
                    exchangeResult.getExchangeRate(),
                    exchangeResult.getDestinationAmount());

            span.setStatus(StatusCode.OK);
            log.info("Exchange completed successfully: {}", exchangeId);

            return ExchangeResponse.builder()
                    .exchangeId(exchangeId)
                    .sourceAccountId(exchangeResult.getSourceAccountId())
                    .destinationAccountId(exchangeResult.getDestinationAccountId())
                    .sourceAmount(exchangeResult.getSourceAmount())
                    .sourceCurrency(exchangeResult.getSourceCurrency())
                    .destinationAmount(exchangeResult.getDestinationAmount())
                    .destinationCurrency(exchangeResult.getDestinationCurrency())
                    .exchangeRate(exchangeResult.getExchangeRate())
                    .status("COMPLETED")
                    .sourceTransactionId(exchangeResult.getSourceTransactionId())
                    .destinationTransactionId(exchangeResult.getDestinationTransactionId())
                    .description(description)
                    .traceId(traceId)
                    .createdAt(exchangeResult.getCreatedAt())
                    .build();

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            log.error("Exchange failed: {} - {}", exchangeId, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
