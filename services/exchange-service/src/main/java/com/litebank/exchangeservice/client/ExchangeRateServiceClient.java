package com.litebank.exchangeservice.client;

import com.litebank.exchangeservice.exception.ExchangeException;
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

    @Value("${services.exchange-rate-service.url}")
    private String exchangeRateServiceUrl;

    @SuppressWarnings("unchecked")
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        try {
            String url = exchangeRateServiceUrl + "/api/v1/rates/" + fromCurrency + "/" + toCurrency;
            log.info("Getting exchange rate: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
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

                log.info("Exchange rate {} -> {}: {}", fromCurrency, toCurrency, rate);
                return rate;
            }

            throw new ExchangeException("ERR_EXC_003", "Failed to get exchange rate");

        } catch (Exception e) {
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_003", "Failed to get exchange rate: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {
        try {
            String url = exchangeRateServiceUrl + "/api/v1/rates/convert?from=" + fromCurrency
                    + "&to=" + toCurrency + "&amount=" + amount;
            log.info("Converting amount: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
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

                log.info("Converted {} {} to {} {}", amount, fromCurrency, convertedAmount, toCurrency);
                return convertedAmount;
            }

            throw new ExchangeException("ERR_EXC_004", "Failed to convert amount");

        } catch (Exception e) {
            if (e instanceof ExchangeException) throw e;
            throw new ExchangeException("ERR_EXC_004", "Failed to convert amount: " + e.getMessage());
        }
    }
}
