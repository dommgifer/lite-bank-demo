package com.litebank.exchangerateservice.service;

import com.litebank.exchangerateservice.dto.ExchangeRateResponse;
import com.litebank.exchangerateservice.exception.CurrencyNotSupportedException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final Tracer tracer;

    // Supported currencies
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "TWD", "USD", "EUR", "JPY", "GBP", "CNY", "HKD", "SGD", "AUD", "KRW"
    );

    // Mock exchange rates (base: USD)
    private static final Map<String, BigDecimal> USD_RATES = new HashMap<>();

    static {
        // Rates as of mock data (1 USD = X currency)
        USD_RATES.put("USD", BigDecimal.ONE);
        USD_RATES.put("TWD", new BigDecimal("31.50"));
        USD_RATES.put("EUR", new BigDecimal("0.92"));
        USD_RATES.put("JPY", new BigDecimal("149.50"));
        USD_RATES.put("GBP", new BigDecimal("0.79"));
        USD_RATES.put("CNY", new BigDecimal("7.24"));
        USD_RATES.put("HKD", new BigDecimal("7.82"));
        USD_RATES.put("SGD", new BigDecimal("1.34"));
        USD_RATES.put("AUD", new BigDecimal("1.53"));
        USD_RATES.put("KRW", new BigDecimal("1320.00"));
    }

    public ExchangeRateResponse getExchangeRate(String fromCurrency, String toCurrency) {
        Span span = tracer.spanBuilder("ExchangeRateService.getExchangeRate")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("from.currency", fromCurrency);
            span.setAttribute("to.currency", toCurrency);

            String from = fromCurrency.toUpperCase();
            String to = toCurrency.toUpperCase();

            log.info("Getting exchange rate: {} -> {}", from, to);

            // Validate currencies
            if (!SUPPORTED_CURRENCIES.contains(from)) {
                span.setStatus(StatusCode.ERROR, "Unsupported currency: " + from);
                throw new CurrencyNotSupportedException("Currency not supported: " + from);
            }
            if (!SUPPORTED_CURRENCIES.contains(to)) {
                span.setStatus(StatusCode.ERROR, "Unsupported currency: " + to);
                throw new CurrencyNotSupportedException("Currency not supported: " + to);
            }

            // Calculate exchange rate
            BigDecimal rate = calculateRate(from, to);
            BigDecimal inverseRate = calculateRate(to, from);

            span.setAttribute("exchange.rate", rate.toString());
            span.setStatus(StatusCode.OK);

            log.info("Exchange rate {} -> {}: {}", from, to, rate);

            return ExchangeRateResponse.builder()
                    .fromCurrency(from)
                    .toCurrency(to)
                    .rate(rate)
                    .inverseRate(inverseRate)
                    .timestamp(Instant.now())
                    .source("MOCK")
                    .build();

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public Set<String> getSupportedCurrencies() {
        Span span = tracer.spanBuilder("ExchangeRateService.getSupportedCurrencies")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("currency.count", SUPPORTED_CURRENCIES.size());
            span.setStatus(StatusCode.OK);
            return SUPPORTED_CURRENCIES;
        } finally {
            span.end();
        }
    }

    public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {
        Span span = tracer.spanBuilder("ExchangeRateService.convertAmount")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("from.currency", fromCurrency);
            span.setAttribute("to.currency", toCurrency);
            span.setAttribute("amount", amount.toString());

            ExchangeRateResponse rateResponse = getExchangeRate(fromCurrency, toCurrency);
            BigDecimal convertedAmount = amount.multiply(rateResponse.getRate())
                    .setScale(2, RoundingMode.HALF_UP);

            span.setAttribute("converted.amount", convertedAmount.toString());
            span.setStatus(StatusCode.OK);

            log.info("Converted {} {} to {} {}", amount, fromCurrency, convertedAmount, toCurrency);

            return convertedAmount;

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private BigDecimal calculateRate(String from, String to) {
        // Convert through USD as base
        BigDecimal fromToUsd = BigDecimal.ONE.divide(USD_RATES.get(from), 10, RoundingMode.HALF_UP);
        BigDecimal usdToTo = USD_RATES.get(to);
        return fromToUsd.multiply(usdToTo).setScale(6, RoundingMode.HALF_UP);
    }
}
