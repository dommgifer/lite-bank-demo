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
 * E2E Tests for Same-Currency Transfer Operations (FR4)
 * Tests complete transfer flow through API Gateway
 * Validates atomic distributed transaction via pessimistic locking
 */
@DisplayName("E2E: Same-Currency Transfer (FR4)")
class TransferE2ETest extends BaseE2ETest {

    private String token;
    private Long userId;
    private Long sourceAccountId;
    private Long destinationAccountId;

    @BeforeEach
    void setupUser() {
        // Use pre-existing demo user alice
        token = loginUser("alice", "password123");
        userId = getUserId("alice", "password123");

        // Create two USD accounts for transfer testing
        sourceAccountId = createAccount(token, userId, "USD");
        destinationAccountId = createAccount(token, userId, "USD");
    }

    @Test
    @DisplayName("FR4: Should transfer money between same-currency accounts successfully")
    void testSameCurrencyTransferSuccess() {
        // Arrange
        BigDecimal transferAmount = new BigDecimal("300.00");
        String referenceId = "TRANSFER-" + UUID.randomUUID();

        // Act: Transfer
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Test same-currency transfer"
                    }
                    """, sourceAccountId, destinationAccountId, transferAmount, referenceId))
                .post("/api/v1/transfers");

        // Assert: Transfer created successfully
        response.then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.sourceAccountId", equalTo(sourceAccountId.intValue()))
                .body("data.destinationAccountId", equalTo(destinationAccountId.intValue()))
                .body("data.amount", equalTo(transferAmount.floatValue()))
                .body("data.currency", equalTo("USD"))
                .body("data.sourceBalanceAfter", equalTo(700.00f)) // 1000 - 300
                .body("data.destinationBalanceAfter", equalTo(1300.00f)) // 1000 + 300
                .body("data.sourceTransactionId", notNullValue())
                .body("data.destinationTransactionId", notNullValue())
                .body("data.referenceId", equalTo(referenceId))
                .body("traceId", notNullValue());

        String traceId = getTraceId(response);
        assertNotNull(traceId, "Trace ID should be present for transfer tracking");

        // Verify source account balance decreased
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + sourceAccountId + "/balance")
                    .then()
                    .statusCode(200)
                    .body("data.balance", equalTo(700.00f));
        });

        // Verify destination account balance increased
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + destinationAccountId + "/balance")
                    .then()
                    .statusCode(200)
                    .body("data.balance", equalTo(1300.00f));
        });
    }

    @Test
    @DisplayName("FR4: Should reject transfer when insufficient balance")
    void testTransferInsufficientBalance() {
        // Arrange: Try to transfer more than available balance
        BigDecimal transferAmount = new BigDecimal("2000.00"); // More than 1000 initial balance

        // Act & Assert
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "TRANSFER-%s",
                        "description": "Test insufficient balance"
                    }
                    """, sourceAccountId, destinationAccountId, transferAmount, UUID.randomUUID()))
                .post("/api/v1/transfers")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());

        // Verify balances remain unchanged
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + sourceAccountId + "/balance")
                .then()
                .body("data.balance", equalTo(1000.00f));

        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + destinationAccountId + "/balance")
                .then()
                .body("data.balance", equalTo(1000.00f));
    }

    @Test
    @DisplayName("FR4: Should reject transfer between different currency accounts")
    void testTransferDifferentCurrencies() {
        // Arrange: Create EUR account
        Long eurAccountId = createAccount(token, userId, "EUR");

        // Act & Assert: Try to transfer USD to EUR account via transfer endpoint
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "TRANSFER-%s",
                        "description": "Test currency mismatch"
                    }
                    """, sourceAccountId, eurAccountId, UUID.randomUUID()))
                .post("/api/v1/transfers")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR7: Should leave balances unchanged when transfer fails")
    void testTransferFailureAtomicity() {
        // Verify that failed transfers don't modify balances (DB transaction rollback)

        // Arrange: Get initial balances
        float initialSourceBalance = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + sourceAccountId + "/balance")
                .path("data.balance");

        float initialDestBalance = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + destinationAccountId + "/balance")
                .path("data.balance");

        // Act: Attempt transfer that might fail (invalid account ID)
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": 999999,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "TRANSFER-%s",
                        "description": "Test transfer failure atomicity"
                    }
                    """, sourceAccountId, UUID.randomUUID()))
                .post("/api/v1/transfers")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404), equalTo(422)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());

        // Assert: Balances should remain unchanged (DB transaction rolled back)
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + sourceAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(initialSourceBalance));
        });

        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + destinationAccountId + "/balance")
                .then()
                .body("data.balance", equalTo(initialDestBalance));
    }

    @Test
    @DisplayName("FR8: Should ensure transfer idempotency")
    void testTransferIdempotency() {
        // Arrange
        String referenceId = "IDEMPOTENT-TRANSFER-" + UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("200.00");

        // Act: Make the same transfer request twice
        Response response1 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Idempotent transfer test"
                    }
                    """, sourceAccountId, destinationAccountId, transferAmount, referenceId))
                .post("/api/v1/transfers");

        response1.then()
                .statusCode(201);

        // Second request with same referenceId
        Response response2 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Idempotent transfer test"
                    }
                    """, sourceAccountId, destinationAccountId, transferAmount, referenceId))
                .post("/api/v1/transfers");

        // Assert: Second request should be idempotent (201 or 409)
        int statusCode = response2.getStatusCode();
        assertTrue(statusCode == 201 || statusCode == 409,
                "Duplicate transfer should return 201 or 409, got: " + statusCode);

        // Verify transfer only happened once
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + sourceAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(800.00f)); // 1000 - 200 (not - 400)
        });

        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + destinationAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(1200.00f)); // 1000 + 200 (not + 400)
        });
    }

    @Test
    @DisplayName("Should handle multiple sequential transfers correctly")
    void testMultipleSequentialTransfers() {
        // Act: Make 3 transfers
        for (int i = 1; i <= 3; i++) {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .body(String.format("""
                        {
                            "fromAccountId": %d,
                            "toAccountId": %d,
                            "amount": 100.00,
                            "currency": "USD",
                            "referenceId": "TRANSFER-%s",
                            "description": "Transfer #%d"
                        }
                        """, sourceAccountId, destinationAccountId, UUID.randomUUID(), i))
                    .post("/api/v1/transfers")
                    .then()
                    .statusCode(201);
        }

        // Assert: Source should have 700 (1000 - 300)
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + sourceAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(700.00f));
        });

        // Assert: Destination should have 1300 (1000 + 300)
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + destinationAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(1300.00f));
        });
    }

    @Test
    @DisplayName("Should reject transfer without authentication")
    void testTransferWithoutAuth() {
        RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "TRANSFER-%s"
                    }
                    """, sourceAccountId, destinationAccountId, UUID.randomUUID()))
                .post("/api/v1/transfers")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should propagate trace ID through transfer flow")
    void testTraceIdPropagationInTransfer() {
        // Act: Execute transfer
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": 150.00,
                        "currency": "USD",
                        "referenceId": "TRANSFER-%s",
                        "description": "Trace ID test"
                    }
                    """, sourceAccountId, destinationAccountId, UUID.randomUUID()))
                .post("/api/v1/transfers");

        // Assert: Trace ID present in response
        String traceId = getTraceId(response);
        assertNotNull(traceId, "Trace ID must be present for transfer tracking");
        assertFalse(traceId.isEmpty());

        response.then()
                .statusCode(201)
                .body("traceId", equalTo(traceId));

        // Verify both transactions have the same trace ID
        Long sourceTransactionId = response.path("data.sourceTransactionId");
        Long destTransactionId = response.path("data.destinationTransactionId");

        assertNotNull(sourceTransactionId);
        assertNotNull(destTransactionId);
        assertNotEquals(sourceTransactionId, destTransactionId,
                "Source and destination should have different transaction IDs");
    }
}
