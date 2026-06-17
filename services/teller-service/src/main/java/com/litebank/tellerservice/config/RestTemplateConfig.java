package com.litebank.tellerservice.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.web.v3_1.SpringWebTelemetry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * P-001 (TEL-W002): downstream calls previously had no timeout, so a delayed
     * dependency pinned a Tomcat thread for the full delay and threads exhausted.
     * connectTimeout 1s / readTimeout 2s makes a delayed call fail-fast and release
     * the thread (normal calls take tens of ms, so 2s is generous).
     */
    @Bean
    public RestTemplate restTemplate(OpenTelemetry openTelemetry, RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .connectTimeout(Duration.ofSeconds(1))
                .readTimeout(Duration.ofSeconds(2))
                .build();
        restTemplate.getInterceptors().add(
            SpringWebTelemetry.create(openTelemetry).newInterceptor()
        );
        return restTemplate;
    }
}
