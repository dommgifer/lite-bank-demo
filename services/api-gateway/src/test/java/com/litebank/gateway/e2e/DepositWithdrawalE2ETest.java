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
 * E2E Tests for Deposit and Withdrawal Operations (FR2, FR3)
 * Tests complete deposit/withdrawal flow through API Gateway
 */
@DisplayName("E2E: Deposit & Withdrawal Operations (FR2, FR3)")
class DepositWithdrawalE2ETest extends BaseE2ETest {

    private String token;
    private Long userId;
    private Long usdAccountId;
    private Long eurAccountId;

    @BeforeEach
    void setupUser() {
        // Use pre-existing demo user alice
        token = loginUser("alice", "password123");
        userId = getUserId("alice", "password123");

        // Create test accounts
        usdAccountId = createAccount(token, userId, "USD");
        eurAccountId = createAccount(token, userId, "EUR");
    }

    @Test
    @DisplayName("FR2: Should deposit money to account successfully")
    void testDepositSuccess() {
        // Arrange
        BigDecimal depositAmount = new BigDecimal("500.00");
        String referenceId = "DEP-" + UUID.randomUUID();

        // Act: Deposit
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Test deposit"
                    }
                    """, usdAccountId, depositAmount, referenceId))
                .post("/api/v1/deposits");

        // Assert
        response.then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.accountId", equalTo(usdAccountId.intValue()))
                .body("data.amount", equalTo(depositAmount.floatValue()))
                .body("data.currency", equalTo("USD"))
                .body("data.balanceAfter", equalTo(1500.00f)) // 1000 initial + 500 deposit
                .body("data.transactionId", notNullValue())
                .body("traceId", notNullValue());

        String traceId = getTraceId(response);
        assertNotNull(traceId);

        // Verify balance updated
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + usdAccountId + "/balance")
                    .then()
                    .statusCode(200)
                    .body("data.balance", equalTo(1500.00f));
        });
    }

    @Test
    @DisplayName("FR3: Should withdraw money from account successfully")
    void testWithdrawalSuccess() {
        // Arrange
        BigDecimal withdrawalAmount = new BigDecimal("300.00");
        String referenceId = "WD-" + UUID.randomUUID();

        // Act: Withdrawal
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Test withdrawal"
                    }
                    """, usdAccountId, withdrawalAmount, referenceId))
                .post("/api/v1/withdrawals");

        // Assert
        response.then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.accountId", equalTo(usdAccountId.intValue()))
                .body("data.amount", equalTo(withdrawalAmount.floatValue()))
                .body("data.currency", equalTo("USD"))
                .body("data.balanceAfter", equalTo(700.00f)) // 1000 initial - 300 withdrawal
                .body("data.transactionId", notNullValue())
                .body("traceId", notNullValue());

        // Verify balance updated
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + usdAccountId + "/balance")
                    .then()
                    .statusCode(200)
                    .body("data.balance", equalTo(700.00f));
        });
    }

    @Test
    @DisplayName("FR3: Should reject withdrawal when insufficient balance")
    void testWithdrawalInsufficientBalance() {
        // Arrange: Try to withdraw more than balance
        BigDecimal withdrawalAmount = new BigDecimal("2000.00"); // More than 1000 initial balance

        // Act & Assert
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": %s,
                        "currency": "USD",
                        "referenceId": "WD-%s",
                        "description": "Test insufficient balance"
                    }
                    """, usdAccountId, withdrawalAmount, UUID.randomUUID()))
                .post("/api/v1/withdrawals")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should handle multiple deposits correctly")
    void testMultipleDeposits() {
        // Act: Make 3 deposits
        for (int i = 1; i <= 3; i++) {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .body(String.format("""
                        {
                            "accountId": %d,
                            "amount": 100.00,
                            "currency": "USD",
                            "referenceId": "DEP-%s",
                            "description": "Deposit #%d"
                        }
                        """, usdAccountId, UUID.randomUUID(), i))
                    .post("/api/v1/deposits")
                    .then()
                    .statusCode(201);
        }

        // Assert: Balance should be 1000 + 300 = 1300
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + usdAccountId + "/balance")
                    .then()
                    .statusCode(200)
                    .body("data.balance", equalTo(1300.00f));
        });
    }

    @Test
    @DisplayName("Should handle deposit and withdrawal sequence correctly")
    void testDepositThenWithdrawal() {
        // Act: Deposit 500
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 500.00,
                        "currency": "EUR",
                        "referenceId": "DEP-%s",
                        "description": "Test deposit"
                    }
                    """, eurAccountId, UUID.randomUUID()))
                .post("/api/v1/deposits")
                .then()
                .statusCode(201);

        // Assert: Balance should be 1500
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + eurAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(1500.00f));
        });

        // Act: Withdraw 800
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 800.00,
                        "currency": "EUR",
                        "referenceId": "WD-%s",
                        "description": "Test withdrawal"
                    }
                    """, eurAccountId, UUID.randomUUID()))
                .post("/api/v1/withdrawals")
                .then()
                .statusCode(201);

        // Assert: Final balance should be 700
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + eurAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(700.00f));
        });
    }

    @Test
    @DisplayName("FR8: Should ensure idempotency - duplicate deposit with same referenceId")
    void testDepositIdempotency() {
        // Arrange
        String referenceId = "IDEMPOTENT-DEP-" + UUID.randomUUID();

        // Act: Make the same deposit twice with same referenceId
        Response response1 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 250.00,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Idempotent deposit test"
                    }
                    """, usdAccountId, referenceId))
                .post("/api/v1/deposits");

        response1.then()
                .statusCode(201);

        Response response2 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 250.00,
                        "currency": "USD",
                        "referenceId": "%s",
                        "description": "Idempotent deposit test"
                    }
                    """, usdAccountId, referenceId))
                .post("/api/v1/deposits");

        // Assert: Second request should either succeed with same result or return 409
        int statusCode = response2.getStatusCode();
        assertTrue(statusCode == 201 || statusCode == 409,
                "Duplicate request should return 201 or 409, got: " + statusCode);

        // Verify balance only increased once
        await().untilAsserted(() -> {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/accounts/" + usdAccountId + "/balance")
                    .then()
                    .body("data.balance", equalTo(1250.00f)); // 1000 + 250 (not + 500)
        });
    }

    @Test
    @DisplayName("Should reject operations without authentication")
    void testWithoutAuthentication() {
        // Test deposit without auth
        RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "DEP-%s"
                    }
                    """, usdAccountId, UUID.randomUUID()))
                .post("/api/v1/deposits")
                .then()
                .statusCode(401);

        // Test withdrawal without auth
        RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "WD-%s"
                    }
                    """, usdAccountId, UUID.randomUUID()))
                .post("/api/v1/withdrawals")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should propagate trace ID through deposit/withdrawal operations")
    void testTraceIdPropagation() {
        // Deposit
        Response depositResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "DEP-%s"
                    }
                    """, usdAccountId, UUID.randomUUID()))
                .post("/api/v1/deposits");

        String depositTraceId = getTraceId(depositResponse);
        assertNotNull(depositTraceId);
        depositResponse.then()
                .body("traceId", equalTo(depositTraceId));

        // Withdrawal
        Response withdrawalResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 50.00,
                        "currency": "USD",
                        "referenceId": "WD-%s"
                    }
                    """, usdAccountId, UUID.randomUUID()))
                .post("/api/v1/withdrawals");

        String withdrawalTraceId = getTraceId(withdrawalResponse);
        assertNotNull(withdrawalTraceId);
        assertNotEquals(depositTraceId, withdrawalTraceId);
        withdrawalResponse.then()
                .body("traceId", equalTo(withdrawalTraceId));
    }
}
