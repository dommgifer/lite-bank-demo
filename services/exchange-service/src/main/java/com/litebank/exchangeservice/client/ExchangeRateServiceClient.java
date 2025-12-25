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
public class ExchangeRateServiceClient {

    private final RestTemplate restTemplate;
    private final Tracer tracer;

    @Value("${services.exchange-rate-service.url}")
    private String exchangeRateServiceUrl;

    @SuppressWarnings("unchecked")
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        Span span = tracer.spanBuilder("ExchangeRateServiceClient.getExchangeRate")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("from.currency", fromCurrency);
            span.setAttribute("to.currency", toCurrency);

            String url = exchangeRateServiceUrl + "/api/v1/rates/" + fromCurrency + "/" + toCurrency;
            log.info("Getting exchange rate: {}", url);

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
                Object rateObj = data.get("rate");
                BigDecimal rate;
                if (rateObj instanceof Number) {
                    rate = new BigDecimal(rateObj.toString());
                } else {
                    rate = new BigDecimal((String) rateObj);
                }

                span.setAttribute("exchange.rate", rate.toString());
                span.setStatus(StatusCode.OK);

                log.info("Exchange rate {} -> {}: {}", fromCurrency, toCurrency, rate);
                return rate;
            }

            span.setStatus(StatusCode.ERROR, "Failed to get exchange rate");
            throw new ExchangeException("ERR_EXC_003", "Failed to get exchange rate");

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_003", "Failed to get exchange rate: " + e.getMessage());
        } finally {
            span.end();
        }
    }

    @SuppressWarnings("unchecked")
    public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {
        Span span = tracer.spanBuilder("ExchangeRateServiceClient.convertAmount")
                .setParent(Context.current())
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("from.currency", fromCurrency);
            span.setAttribute("to.currency", toCurrency);
            span.setAttribute("amount", amount.toString());

            String url = exchangeRateServiceUrl + "/api/v1/rates/convert?from=" + fromCurrency
                    + "&to=" + toCurrency + "&amount=" + amount;
            log.info("Converting amount: {}", url);

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
                Object convertedObj = data.get("convertedAmount");
                BigDecimal convertedAmount;
                if (convertedObj instanceof Number) {
                    convertedAmount = new BigDecimal(convertedObj.toString());
                } else {
                    convertedAmount = new BigDecimal((String) convertedObj);
                }

                span.setAttribute("converted.amount", convertedAmount.toString());
                span.setStatus(StatusCode.OK);

                log.info("Converted {} {} to {} {}", amount, fromCurrency, convertedAmount, toCurrency);
                return convertedAmount;
            }

            span.setStatus(StatusCode.ERROR, "Failed to convert amount");
            throw new ExchangeException("ERR_EXC_004", "Failed to convert amount");

        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_004", "Failed to convert amount: " + e.getMessage());
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
