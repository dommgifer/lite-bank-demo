package com.litebank.gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Tests for Cross-Currency Exchange Operations (FR5, FR6)
 * Tests currency exchange with real-time rates through API Gateway
 */
@DisplayName("E2E: Currency Exchange (FR5, FR6)")
class ExchangeE2ETest extends BaseE2ETest {

    private String token;
    private Long userId;
    private Long usdAccountId;
    private Long eurAccountId;
    private Long twdAccountId;

    @BeforeEach
    void setupUser() {
        // Use pre-existing demo user alice
        token = loginUser("alice", "password123");
        userId = getUserId("alice", "password123");

        // Create multi-currency accounts
        usdAccountId = createAccount(token, userId, "USD");
        eurAccountId = createAccount(token, userId, "EUR");
        twdAccountId = createAccount(token, userId, "TWD");
    }

    @Test
    @DisplayName("FR6: Should query exchange rate successfully")
    void testQueryExchangeRate() {
        // Act: Query USD to EUR exchange rate
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/rates/USD/EUR");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.fromCurrency", equalTo("USD"))
                .body("data.toCurrency", equalTo("EUR"))
                .body("data.rate", notNullValue())
                .body("data.rate", greaterThan(0.0f))
                .body("traceId", notNullValue());

        float rate = response.path("data.rate");
        assertTrue(rate > 0, "Exchange rate should be positive");
    }

    @Test
    @DisplayName("FR6: Should query all supported currencies")
    void testQuerySupportedCurrencies() {
        // Act
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/rates/currencies");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(greaterThan(0)))
                .body("data", hasItems("USD", "EUR", "TWD", "GBP", "JPY"))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR6: Should convert amount using exchange rate")
    void testConvertAmount() {
        // Act: Convert 100 USD to EUR
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("from", "USD")
                .queryParam("to", "EUR")
                .queryParam("amount", 100.00)
                .get("/api/v1/rates/convert");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.fromCurrency", equalTo("USD"))
                .body("data.toCurrency", equalTo("EUR"))
                .body("data.originalAmount", equalTo(100.0f))
                .body("data.convertedAmount", notNullValue())
                .body("data.convertedAmount", greaterThan(0.0f))
                .body("data.rate", notNullValue())
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR5: Should exchange currency between accounts successfully")
    void testCurrencyExchangeSuccess() {
        // Arrange: Get exchange rate first
        float exchangeRate = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/rates/USD/EUR")
                .path("data.rate");

        BigDecimal sourceAmount = new BigDecimal("200.00");
        BigDecimal destinationAmount = sourceAmount.multiply(BigDecimal.valueOf(exchangeRate))
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        String referenceId = "EXCHANGE-" + UUID.randomUUID();

        // Act: Exchange USD to EUR
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": %s,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "EUR",
                        "referenceId": "%s",
                        "description": "Test currency exchange"
                    }
                    """, usdAccountId, eurAccountId, sourceAmount, referenceId))
                .post("/api/v1/exchanges");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.sourceAccountId", equalTo(usdAccountId.intValue()))
                .body("data.destinationAccountId", equalTo(eurAccountId.intValue()))
                .body("data.sourceAmount", equalTo(sourceAmount.floatValue()))
                .body("data.sourceCurrency", equalTo("USD"))
                .body("data.destinationCurrency", equalTo("EUR"))
                .body("data.destinationAmount", notNullValue())
                .body("data.exchangeRate", equalTo(exchangeRate))
                .body("data.sourceBalanceAfter", equalTo(800.00f)) // 1000 - 200
                .body("data.sourceTransactionId", notNullValue())
                .body("data.destinationTransactionId", notNullValue())
                .body("traceId", notNullValue());

        String traceId = getTraceId(response);
        assertNotNull(traceId);

        // Verify source account balance decreased
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + usdAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(800.00f));
        });

        // Verify destination account balance increased
        await().untilAsserted(() -> {
            float eurBalance = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + eurAccountId + "/balance")
                    .path("data.balance");

            assertTrue(eurBalance > 1000.00f, "EUR balance should increase after exchange");
        });
    }

    @Test
    @DisplayName("FR5: Should reject exchange when insufficient balance")
    void testExchangeInsufficientBalance() {
        // Arrange: Try to exchange more than available
        BigDecimal sourceAmount = new BigDecimal("2000.00");

        // Act & Assert
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": %s,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "EUR",
                        "referenceId": "EXCHANGE-%s",
                        "description": "Test insufficient balance"
                    }
                    """, usdAccountId, eurAccountId, sourceAmount, UUID.randomUUID()))
                .post("/api/v1/exchanges")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR5: Should reject exchange between same currency accounts")
    void testExchangeSameCurrency() {
        // Arrange: Create another USD account
        Long anotherUsdAccountId = createAccount(token, userId, "USD");

        // Act & Assert: Try to exchange USD to USD
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": 100.00,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "USD",
                        "referenceId": "EXCHANGE-%s",
                        "description": "Test same currency"
                    }
                    """, usdAccountId, anotherUsdAccountId, UUID.randomUUID()))
                .post("/api/v1/exchanges")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR5: Should handle USD to TWD exchange correctly")
    void testUsdToTwdExchange() {
        // Act: Exchange USD to TWD
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": 100.00,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "TWD",
                        "referenceId": "EXCHANGE-%s",
                        "description": "USD to TWD exchange"
                    }
                    """, usdAccountId, twdAccountId, UUID.randomUUID()))
                .post("/api/v1/exchanges");

        // Assert: Should succeed
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.sourceCurrency", equalTo("USD"))
                .body("data.destinationCurrency", equalTo("TWD"))
                .body("data.destinationAmount", greaterThan(100.0f)) // TWD should be more than USD
                .body("traceId", notNullValue());

        // Verify TWD account balance increased significantly
        await().untilAsserted(() -> {
            float twdBalance = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + twdAccountId + "/balance")
                    .path("data.balance");

            assertTrue(twdBalance > 1000.00f, "TWD balance should increase significantly");
        });
    }

    @Test
    @DisplayName("FR8: Should ensure exchange idempotency")
    void testExchangeIdempotency() {
        // Arrange
        String referenceId = "IDEMPOTENT-EXCHANGE-" + UUID.randomUUID();

        // Act: Make the same exchange request twice
        Response response1 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": 150.00,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "EUR",
                        "referenceId": "%s",
                        "description": "Idempotent exchange test"
                    }
                    """, usdAccountId, eurAccountId, referenceId))
                .post("/api/v1/exchanges");

        response1.then()
                .statusCode(200);

        Response response2 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": 150.00,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "EUR",
                        "referenceId": "%s",
                        "description": "Idempotent exchange test"
                    }
                    """, usdAccountId, eurAccountId, referenceId))
                .post("/api/v1/exchanges");

        // Assert: Second request should be idempotent
        int statusCode = response2.getStatusCode();
        assertTrue(statusCode == 200 || statusCode == 409,
                "Duplicate exchange should return 200 or 409, got: " + statusCode);

        // Verify exchange only happened once
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + usdAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(850.00f)); // 1000 - 150 (not - 300)
        });
    }

    @Test
    @DisplayName("Should reject exchange without authentication")
    void testExchangeWithoutAuth() {
        RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": 100.00,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "EUR",
                        "referenceId": "EXCHANGE-%s"
                    }
                    """, usdAccountId, eurAccountId, UUID.randomUUID()))
                .post("/api/v1/exchanges")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should propagate trace ID through exchange operations")
    void testTraceIdPropagation() {
        // Act: Get exchange rate
        Response rateResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/rates/USD/EUR");

        String rateTraceId = getTraceId(rateResponse);
        assertNotNull(rateTraceId);
        rateResponse.then()
                .body("traceId", equalTo(rateTraceId));

        // Act: Execute exchange
        Response exchangeResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "sourceAccountId": %d,
                        "destinationAccountId": %d,
                        "sourceAmount": 50.00,
                        "sourceCurrency": "USD",
                        "destinationCurrency": "EUR",
                        "referenceId": "EXCHANGE-%s"
                    }
                    """, usdAccountId, eurAccountId, UUID.randomUUID()))
                .post("/api/v1/exchanges");

        String exchangeTraceId = getTraceId(exchangeResponse);
        assertNotNull(exchangeTraceId);
        assertNotEquals(rateTraceId, exchangeTraceId, "Different requests should have different trace IDs");
        exchangeResponse.then()
                .body("traceId", equalTo(exchangeTraceId));
    }
}
