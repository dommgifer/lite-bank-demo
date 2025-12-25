package com.litebank.gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Tests for Account Management (FR1: Query Accounts)
 * Tests complete account creation and query flow through API Gateway
 */
@DisplayName("E2E: Account Management (FR1)")
class AccountManagementE2ETest extends BaseE2ETest {

    private String token;
    private Long userId;

    @BeforeEach
    void setupUser() {
        // Use pre-existing demo user alice
        token = loginUser("alice", "password123");
        userId = getUserId("alice", "password123");
    }

    @Test
    @DisplayName("FR1: Should create new account successfully")
    void testCreateAccount() {
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "userId": %d,
                        "currency": "USD",
                        "initialBalance": 1000.00
                    }
                    """, userId))
                .post("/api/v1/accounts");

        response.then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.accountId", notNullValue())
                .body("data.userId", equalTo(userId.intValue()))
                .body("data.currency", equalTo("USD"))
                .body("data.balance", equalTo(1000.00f))
                .body("traceId", notNullValue());

        String traceId = getTraceId(response);
        assertNotNull(traceId, "Trace ID should be present");
    }

    @Test
    @DisplayName("FR1: Should query all accounts for user")
    void testQueryAccountsByUser() {
        // Arrange: Create multiple accounts with different currencies
        createAccount(token, userId, "USD");
        createAccount(token, userId, "EUR");
        createAccount(token, userId, "TWD");

        // Act: Query all accounts
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("userId", userId)
                .get("/api/v1/accounts");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(greaterThanOrEqualTo(3)))
                .body("data[0].accountId", notNullValue())
                .body("data[0].userId", equalTo(userId.intValue()))
                .body("data[0].currency", notNullValue())
                .body("data[0].balance", notNullValue())
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR1: Should query specific account by ID")
    void testQueryAccountById() {
        // Arrange: Create an account
        Long accountId = createAccount(token, userId, "JPY");

        // Act: Query specific account
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + accountId);

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.accountId", equalTo(accountId.intValue()))
                .body("data.userId", equalTo(userId.intValue()))
                .body("data.currency", equalTo("JPY"))
                .body("data.balance", equalTo(1000.00f))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("FR1: Should query account balance")
    void testQueryAccountBalance() {
        // Arrange: Create an account
        Long accountId = createAccount(token, userId, "GBP");

        // Act: Query balance
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + accountId + "/balance");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.accountId", equalTo(accountId.intValue()))
                .body("data.balance", equalTo(1000.00f))
                .body("data.currency", equalTo("GBP"))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should support multiple currency accounts")
    void testMultipleCurrencyAccounts() {
        // Arrange: Create accounts in different currencies
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "TWD", "CNY"};

        for (String currency : currencies) {
            createAccount(token, userId, currency);
        }

        // Act: Query all accounts
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("userId", userId)
                .get("/api/v1/accounts");

        // Assert: Should have at least 6 accounts
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(greaterThanOrEqualTo(6)));
    }

    @Test
    @DisplayName("Should reject account creation without authentication")
    void testCreateAccountWithoutAuth() {
        Response response = RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "userId": %d,
                        "currency": "USD",
                        "initialBalance": 1000.00
                    }
                    """, userId))
                .post("/api/v1/accounts");

        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should reject account query for non-existent account")
    void testQueryNonExistentAccount() {
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/999999");

        response.then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should propagate trace ID through account operations")
    void testTraceIdPropagation() {
        // Act: Create account
        Response createResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "userId": %d,
                        "currency": "USD",
                        "initialBalance": 1000.00
                    }
                    """, userId))
                .post("/api/v1/accounts");

        String createTraceId = getTraceId(createResponse);
        assertNotNull(createTraceId);

        createResponse.then()
                .body("traceId", equalTo(createTraceId));

        // Act: Query accounts (different request, different trace ID)
        Response queryResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("userId", userId)
                .get("/api/v1/accounts");

        String queryTraceId = getTraceId(queryResponse);
        assertNotNull(queryTraceId);
        assertNotEquals(createTraceId, queryTraceId, "Different requests should have different trace IDs");

        queryResponse.then()
                .body("traceId", equalTo(queryTraceId));
    }

    @Test
    @DisplayName("Should maintain data consistency across multiple queries")
    void testDataConsistency() {
        // Arrange: Create account
        Long accountId = createAccount(token, userId, "USD");

        // Act: Query same account multiple times
        Response response1 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + accountId);

        Response response2 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts/" + accountId);

        // Assert: Data should be consistent
        float balance1 = response1.path("data.balance");
        float balance2 = response2.path("data.balance");

        assertEquals(balance1, balance2, "Balance should be consistent across queries");
        assertEquals(1000.00f, balance1, "Initial balance should be 1000.00");
    }
}
