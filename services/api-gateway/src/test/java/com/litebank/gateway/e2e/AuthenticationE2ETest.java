package com.litebank.gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E Tests for User Authentication Flow
 * Tests user registration, login, and JWT token validation through API Gateway
 */
@DisplayName("E2E: Authentication Flow")
class AuthenticationE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("Should login with demo user successfully")
    void testDemoUserLogin() {
        // Test login with pre-existing demo user alice
        Response response = RestAssured.given(requestSpec)
                .body("""
                    {
                        "username": "alice",
                        "password": "password123"
                    }
                    """)
                .post("/api/v1/auth/login");

        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.token", notNullValue())
                .body("data.userId", notNullValue())
                .body("data.username", equalTo("alice"))
                .body("traceId", notNullValue());

        // Verify trace ID is present in response header
        String traceId = getTraceId(response);
        assertNotNull(traceId, "Trace ID should be present in response headers");
        assertFalse(traceId.isEmpty(), "Trace ID should not be empty");
    }

    @Test
    @DisplayName("Should login with valid credentials and receive JWT token")
    void testUserLogin() {
        // Test login with demo user bob
        Response response = RestAssured.given(requestSpec)
                .body("""
                    {
                        "username": "bob",
                        "password": "password123"
                    }
                    """)
                .post("/api/v1/auth/login");

        // Assert
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.token", notNullValue())
                .body("data.userId", notNullValue())
                .body("data.username", equalTo("bob"))
                .body("traceId", notNullValue());

        String token = response.path("data.token");
        assertNotNull(token, "JWT token should be present");
        assertTrue(token.split("\\.").length == 3, "JWT token should have 3 parts");
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void testLoginWithInvalidCredentials() {
        Response response = RestAssured.given(requestSpec)
                .body("""
                    {
                        "username": "nonexistent_user",
                        "password": "WrongPassword123!"
                    }
                    """)
                .post("/api/v1/auth/login");

        response.then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("error", notNullValue())
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should reject duplicate username registration")
    void testDuplicateUsernameRegistration() {
        // Arrange: Register user first
        String username = "duplicate_" + System.currentTimeMillis();
        String email = username + "@example.com";
        String password = "SecurePass123!";

        registerAndLogin(username, email, password);

        // Act: Try to register again with same username
        Response response = RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "username": "%s",
                        "email": "different_%s",
                        "password": "%s"
                    }
                    """, username, email, password))
                .post("/api/v1/auth/register");

        // Assert
        response.then()
                .statusCode(anyOf(equalTo(400), equalTo(409)))
                .body("success", equalTo(false))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should reject request without JWT token for protected endpoints")
    void testProtectedEndpointWithoutToken() {
        Response response = RestAssured.given(requestSpec)
                .get("/api/v1/accounts?userId=1");

        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should reject request with invalid JWT token")
    void testProtectedEndpointWithInvalidToken() {
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer invalid.jwt.token")
                .get("/api/v1/accounts?userId=1");

        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should accept request with valid JWT token")
    void testProtectedEndpointWithValidToken() {
        // Arrange: Login with demo user to get valid token
        String token = loginUser("charlie", "password123");
        Long userId = getUserId("charlie", "password123");

        // Act: Access protected endpoint with valid token
        Response response = RestAssured.given(requestSpec)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/accounts?userId=" + userId);

        // Assert: Should get 200 OK (even if no accounts exist yet)
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("traceId", notNullValue());
    }

    @Test
    @DisplayName("Should propagate trace ID through authentication flow")
    void testTraceIdPropagation() {
        // Arrange
        String username = "traceid_" + System.currentTimeMillis();
        String email = username + "@example.com";

        // Act: Register
        Response registerResponse = RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "username": "%s",
                        "email": "%s",
                        "password": "SecurePass123!"
                    }
                    """, username, email))
                .post("/api/v1/auth/register");

        // Assert: Trace ID should be present
        String registerTraceId = getTraceId(registerResponse);
        assertNotNull(registerTraceId);
        registerResponse.then()
                .body("traceId", equalTo(registerTraceId));

        // Act: Login
        Response loginResponse = RestAssured.given(requestSpec)
                .body(String.format("""
                    {
                        "username": "%s",
                        "password": "SecurePass123!"
                    }
                    """, username))
                .post("/api/v1/auth/login");

        // Assert: Different trace ID for different request
        String loginTraceId = getTraceId(loginResponse);
        assertNotNull(loginTraceId);
        assertNotEquals(registerTraceId, loginTraceId, "Each request should have unique trace ID");
        loginResponse.then()
                .body("traceId", equalTo(loginTraceId));
    }
}
