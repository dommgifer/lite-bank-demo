package com.litebank.userservice.client;

import com.litebank.userservice.exception.RegistrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.account-service.url}")
    private String accountServiceUrl;

    public void createDefaultAccount(Long userId) {
        String url = accountServiceUrl + "/api/v1/accounts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "userId", userId,
                "currency", "TWD"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("Creating default TWD account for userId: {}", userId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to create default account for userId: {}, status: {}", userId, response.getStatusCode());
                throw new RegistrationException("Registration failed, please try again later");
            }

            log.info("Default TWD account created successfully for userId: {}", userId);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create default account for userId: {}, error: {}", userId, e.getMessage());
            throw new RegistrationException("Registration failed, please try again later");
        }
    }
}
