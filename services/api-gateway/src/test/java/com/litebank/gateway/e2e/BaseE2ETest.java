package com.litebank.gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for End-to-End tests
 * Tests against a running API Gateway instance
 *
 * Test Strategy:
 * - Connects to API Gateway running on localhost:9000
 * - Tests complete user journeys through API Gateway
 * - Validates JWT authentication, routing, tracing, and business logic
 *
 * Prerequisites:
 * - All services must be running via docker-compose
 * - API Gateway must be accessible on localhost:9000
 */
public abstract class BaseE2ETest {

    protected static final String API_GATEWAY_BASE_URL =
        System.getProperty("api.gateway.url", "http://localhost:9000");

    protected static RequestSpecification requestSpec;
    protected String authToken;

    @BeforeAll
    static void setupRestAssured() {
        RestAssured.baseURI = API_GATEWAY_BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(API_GATEWAY_BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.ALL)
                .build();
    }

    @BeforeEach
    void resetAuth() {
        authToken = null;
    }

    /**
     * Helper method to login using pre-existing demo users
     * Demo users: alice, bob, charlie (password: password123)
     * These users are created by database migration V1__create_users_table.sql
     * Returns the JWT token
     */
    protected String loginUser(String username, String password) {
        Response loginResponse = RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """, username, password))
                .post("/api/v1/auth/login");

        loginResponse.then()
                .statusCode(200);

        return loginResponse.path("data.token");
    }

    /**
     * Helper method to get user ID from login
     * Returns the userId for the demo user
     */
    protected Long getUserId(String username, String password) {
        Response loginResponse = RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """, username, password))
                .post("/api/v1/auth/login");

        loginResponse.then()
                .statusCode(200);

        // REST Assured returns userId as Integer, need to convert to Long
        Integer userIdInt = loginResponse.path("data.userId");
        return userIdInt.longValue();
    }

    /**
     * Helper method for backward compatibility - uses demo user alice
     */
    protected String registerAndLogin(String username, String email, String password) {
        // Use pre-existing demo user 'alice' instead of registering
        // This is because the system design does not include user registration
        return loginUser("alice", "password123");
    }

    /**
     * Helper method to create an account, returning the account ID
     */
    protected Long createAccount(String token, Long userId, String currency) {
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .body(String.format("""
                    {
                        "userId": %d,
                        "currency": "%s",
                        "initialBalance": 1000.00
                    }
                    """, userId, currency))
                .post("/api/v1/accounts");

        response.then()
                .statusCode(201);

        // REST Assured returns accountId as Integer, need to convert to Long
        Integer accountIdInt = response.path("data.accountId");
        return accountIdInt.longValue();
    }

    /**
     * Extract trace ID from response header
     */
    protected String getTraceId(Response response) {
        // Try X-Trace-Id header first
        String traceId = response.getHeader("X-Trace-Id");
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }

        // Fall back to response body
        try {
            return response.path("traceId");
        } catch (Exception e) {
            return null;
        }
    }
}
