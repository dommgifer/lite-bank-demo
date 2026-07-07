package com.litebank.tellerservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TransactionServiceResilienceConfigTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void transactionServiceCircuitBreakerUsesStableRecoverySettings() {
        CircuitBreakerConfig config = circuitBreakerRegistry
                .circuitBreaker("transactionService")
                .getCircuitBreakerConfig();

        assertThat(config.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        assertThat(config.getSlidingWindowSize()).isEqualTo(50);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(20);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(Duration.ofSeconds(30).toMillis());
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(10);
        assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(1500));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(80.0f);
        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
    }
}
