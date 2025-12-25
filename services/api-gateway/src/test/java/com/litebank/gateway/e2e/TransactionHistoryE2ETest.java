package com.litebank.gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Tests for Transaction History Query
 * Tests transaction record querying, filtering, and trace ID tracking
 */
@DisplayName("E2E: Transaction History & Audit Trail")
class TransactionHistoryE2ETest extends BaseE2ETest {

    private String token;
    private Long userId;
    private Long accountId;
    private String depositTraceId;
    private String transferTraceId;

    @BeforeEach
    void setupUser() {
        // Use pre-existing demo user alice
        token = loginUser("alice", "password123");
        userId = getUserId("alice", "password123");

        // Create test account
        accountId = createAccount(token, userId, "USD");

        // Create some transactions for testing
        createTestTransactions();
    }

    private void createTestTransactions() {
        // Deposit 1
        Response depositResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 500.00,
                        "currency": "USD",
                        "referenceId": "DEP-%s",
                        "description": "Test deposit 1"
                    }
                    """, accountId, UUID.randomUUID()))
                .post("/api/v1/deposits");

        depositTraceId = getTraceId(depositResponse);

        // Withdrawal
        RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "accountId": %d,
                        "amount": 200.00,
                        "currency": "USD",
                        "referenceId": "WD-%s",
                        "description": "Test withdrawal"
                    }
                    """, accountId, UUID.randomUUID()))
                .post("/api/v1/withdrawals");

        // Create second account for transfer
        Long account2Id = createAccount(token, userId, "USD");

        // Transfer
        Response transferResponse = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "fromAccountId": %d,
                        "toAccountId": %d,
                        "amount": 100.00,
                        "currency": "USD",
                        "referenceId": "TRANSFER-%s",
                        "description": "Test transfer"
                    }
                    """, accountId, account2Id, UUID.randomUUID()))
                .post("/api/v1/transfers");

        transferTraceId = getTraceId(transferResponse);
    }

    @Test
    @DisplayName("Should query all transactions for an account")
    void testQueryAllTransactions() {
        // Act
        await().untilAsserted(() -> {
            Response response = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .queryParam("accountId", accountId)
                    .queryParam("page", 0)
                    .queryParam("size", 20)
                    .get("/api/v1/transactions");

            // Assert
            response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.content", hasSize(greaterThanOrEqualTo(3))) // deposit + withdrawal + transfer
                    .body("data.content[0].transactionId", notNullValue())
                    .body("data.content[0].accountId", equalTo(accountId.intValue()))
                    .body("data.content[0].amount", notNullValue())
                    .body("data.content[0].transactionType", notNullValue())
                    .body("data.content[0].traceId", notNullValue())
                    .body("data.page", equalTo(0))
                    .body("data.size", equalTo(20))
                    .body("data.totalElements", greaterThanOrEqualTo(3))
                    .body("traceId", notNullValue());
        });
    }

    @Test
    @DisplayName("Should query transactions by type")
    void testQueryTransactionsByType() {
        // Act: Query DEPOSIT transactions only
        await().untilAsserted(() -> {
            Response response = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .queryParam("accountId", accountId)
                    .queryParam("transactionType", "DEPOSIT")
                    .get("/api/v1/transactions");

            // Assert
            response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.content", hasSize(greaterThanOrEqualTo(1)))
                    .body("data.content[0].transactionType", equalTo("DEPOSIT"));
        });
    }

    @Test
    @DisplayName("FR11: Should query transactions by trace ID")
    void testQueryTransactionsByTraceId() {
        // Act: Query by transfer trace ID (should return 2 transactions: OUT + IN)
        await().untilAsserted(() -> {
            Response response = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/transactions/trace/" + transferTraceId);

            // Assert
            response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", hasSize(2)) // OUT + IN transactions
                    .body("data[0].traceId", equalTo(transferTraceId))
                    .body("data[1].traceId", equalTo(transferTraceId));

            // Verify one is TRANSFER_OUT and one is TRANSFER_IN
            String type1 = response.path("data[0].transactionType");
            String type2 = response.path("data[1].transactionType");

            assertTrue(
                    (type1.equals("TRANSFER_OUT") && type2.equals("TRANSFER_IN")) ||
                    (type1.equals("TRANSFER_IN") && type2.equals("TRANSFER_OUT")),
                    "Should have one TRANSFER_OUT and one TRANSFER_IN"
            );
        });
    }

    @Test
    @DisplayName("FR11: Should include complete transaction metadata")
    void testTransactionMetadata() {
        // Act
        await().untilAsserted(() -> {
            Response response = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .queryParam("accountId", accountId)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .get("/api/v1/transactions");

            // Assert: Verify transaction has all required fields
            response.then()
                    .statusCode(200)
                    .body("data.content[0].transactionId", notNullValue())
                    .body("data.content[0].traceId", notNullValue())
                    .body("data.content[0].accountId", equalTo(accountId.intValue()))
                    .body("data.content[0].amount", notNullValue())
                    .body("data.content[0].currency", equalTo("USD"))
                    .body("data.content[0].transactionType", notNullValue())
                    .body("data.content[0].status", notNullValue())
                    .body("data.content[0].balanceAfter", notNullValue())
                    .body("data.content[0].createdAt", notNullValue())
                    .body("data.content[0].description", notNullValue());
        });
    }

    @Test
    @DisplayName("FR12: Transactions should be immutable (audit trail)")
    void testTransactionImmutability() {
        // Arrange: Get a transaction ID
        Long transactionId = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("accountId", accountId)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .get("/api/v1/transactions")
                .path("data.content[0].transactionId");

        // Act: Query the same transaction multiple times
        Response response1 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/transactions/" + transactionId);

        Response response2 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/transactions/" + transactionId);

        // Assert: Data should be exactly the same (immutable)
        String tx1 = response1.getBody().asString();
        String tx2 = response2.getBody().asString();

        assertEquals(tx1, tx2, "Transaction data should be immutable");
    }

    @Test
    @DisplayName("Should support pagination for transaction queries")
    void testTransactionPagination() {
        // Act: Get first page
        Response page1 = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("accountId", accountId)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .get("/api/v1/transactions");

        page1.then()
                .statusCode(200)
                .body("data.page", equalTo(0))
                .body("data.size", equalTo(2))
                .body("data.content", hasSize(lessThanOrEqualTo(2)));

        // Act: Get second page if exists
        int totalPages = page1.path("data.totalPages");
        if (totalPages > 1) {
            RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .queryParam("accountId", accountId)
                    .queryParam("page", 1)
                    .queryParam("size", 2)
                    .get("/api/v1/transactions")
                    .then()
                    .statusCode(200)
                    .body("data.page", equalTo(1));
        }
    }

    @Test
    @DisplayName("Should query transactions within date range")
    void testQueryTransactionsByDateRange() {
        // Arrange: Get current time range
        String startDate = java.time.LocalDateTime.now().minusDays(1).toString();
        String endDate = java.time.LocalDateTime.now().plusDays(1).toString();

        // Act
        await().untilAsserted(() -> {
            Response response = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .queryParam("accountId", accountId)
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                    .get("/api/v1/transactions");

            // Assert
            response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data.content", hasSize(greaterThanOrEqualTo(1)));
        });
    }

    @Test
    @DisplayName("Should reject transaction query without authentication")
    void testQueryWithoutAuth() {
        RestAssured.given(requestSpec)
                .queryParam("accountId", accountId)
                .get("/api/v1/transactions")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("FR15: Should preserve trace ID in transaction records for audit")
    void testTraceIdPreservationInRecords() {
        // Act: Query transaction by its trace ID
        await().untilAsserted(() -> {
            Response response = RestAssured.given(requestSpec)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/transactions/trace/" + depositTraceId);

            // Assert: Trace ID should be preserved in transaction record
            response.then()
                    .statusCode(200)
                    .body("success", equalTo(true))
                    .body("data", hasSize(greaterThanOrEqualTo(1)))
                    .body("data[0].traceId", equalTo(depositTraceId));

            // Verify trace ID matches the one from original deposit
            String storedTraceId = response.path("data[0].traceId");
            assertEquals(depositTraceId, storedTraceId,
                    "Trace ID should be preserved in transaction record for audit purposes");
        });
    }

    @Test
    @DisplayName("Should query specific transaction by ID")
    void testQueryTransactionById() {
        // Arrange: Get a transaction ID first
        Long transactionId = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("accountId", accountId)
                .queryParam("page", 0)
                .queryParam("size", 1)
                .get("/api/v1/transactions")
                .path("data.content[0].transactionId");

        // Act
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/transactions/" + transactionId);

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.transactionId", equalTo(transactionId.intValue()))
                .body("data.traceId", notNullValue())
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should propagate trace ID in query operations")
    void testTraceIdInQueryOperations() {
        // Act: Query transactions
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .queryParam("accountId", accountId)
                .get("/api/v1/transactions");

        // Assert: Query itself should have trace ID
        String queryTraceId = getTraceId(response);
        assertNotNull(queryTraceId);

        response.then()
                .statusCode(200)
                .body("traceId", equalTo(queryTraceId));

        // Note: Transaction records have their OWN trace IDs from when they were created
        String recordTraceId = response.path("data.content[0].traceId");
        assertNotNull(recordTraceId);
        assertNotEquals(queryTraceId, recordTraceId,
                "Query trace ID should be different from record's original trace ID");
    }
}
