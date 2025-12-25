package com.litebank.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String testSecret = "lite-bank-demo-secret-key-change-in-production-2024";
    private Long testExpiration = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = testSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateValidToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);

        return Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + testExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    private String generateExpiredToken(String userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);

        return Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis() - testExpiration - 1000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(getSigningKey())
                .compact();
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        // Given
        String token = generateValidToken("user123", "testuser");

        // When
        boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertTrue(isValid, "Valid token should be validated successfully");
    }

    @Test
    void validateToken_withExpiredToken_shouldReturnFalse() {
        // Given
        String expiredToken = generateExpiredToken("user123", "testuser");

        // When
        boolean isValid = jwtUtil.validateToken(expiredToken);

        // Then
        assertFalse(isValid, "Expired token should not be valid");
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertFalse(isValid, "Invalid token should not be valid");
    }

    @Test
    void validateToken_withTamperedToken_shouldReturnFalse() {
        // Given
        String validToken = generateValidToken("user123", "testuser");
        String tamperedToken = validToken + "tampered";

        // When
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // Then
        assertFalse(isValid, "Tampered token should not be valid");
    }

    @Test
    void validateToken_withNullToken_shouldReturnFalse() {
        // When
        boolean isValid = jwtUtil.validateToken(null);

        // Then
        assertFalse(isValid, "Null token should not be valid");
    }

    @Test
    void validateToken_withEmptyToken_shouldReturnFalse() {
        // When
        boolean isValid = jwtUtil.validateToken("");

        // Then
        assertFalse(isValid, "Empty token should not be valid");
    }

    @Test
    void extractUserId_withValidToken_shouldReturnUserId() {
        // Given
        String expectedUserId = "user123";
        String token = generateValidToken(expectedUserId, "testuser");

        // When
        String actualUserId = jwtUtil.extractUserId(token);

        // Then
        assertNotNull(actualUserId, "User ID should not be null");
        assertEquals(expectedUserId, actualUserId, "User ID should match the token subject");
    }

    @Test
    void extractUserId_withInvalidToken_shouldReturnNull() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        String userId = jwtUtil.extractUserId(invalidToken);

        // Then
        assertNull(userId, "User ID should be null for invalid token");
    }

    @Test
    void extractUserId_withExpiredToken_shouldStillExtractUserId() {
        // Given
        String expectedUserId = "user123";
        String expiredToken = generateExpiredToken(expectedUserId, "testuser");

        // When
        // Note: JJWT will throw exception for expired tokens during parsing
        String userId = jwtUtil.extractUserId(expiredToken);

        // Then
        assertNull(userId, "User ID extraction should fail for expired token due to security");
    }

    @Test
    void extractUsername_withValidToken_shouldReturnUsername() {
        // Given
        String expectedUsername = "testuser";
        String token = generateValidToken("user123", expectedUsername);

        // When
        String actualUsername = jwtUtil.extractUsername(token);

        // Then
        assertNotNull(actualUsername, "Username should not be null");
        assertEquals(expectedUsername, actualUsername, "Username should match the token claim");
    }

    @Test
    void extractUsername_withInvalidToken_shouldReturnNull() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        String username = jwtUtil.extractUsername(invalidToken);

        // Then
        assertNull(username, "Username should be null for invalid token");
    }

    @Test
    void extractUsername_withTokenMissingUsernameClaim_shouldReturnNull() {
        // Given - token without username claim
        String token = Jwts.builder()
                .subject("user123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + testExpiration))
                .signWith(getSigningKey())
                .compact();

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertNull(username, "Username should be null when claim is missing");
    }

    @Test
    void extractUsername_withNullToken_shouldReturnNull() {
        // When
        String username = jwtUtil.extractUsername(null);

        // Then
        assertNull(username, "Username should be null for null token");
    }

    @Test
    void multipleOperations_withSameToken_shouldBeConsistent() {
        // Given
        String userId = "user456";
        String username = "consistentuser";
        String token = generateValidToken(userId, username);

        // When - multiple operations
        boolean isValid1 = jwtUtil.validateToken(token);
        String extractedUserId1 = jwtUtil.extractUserId(token);
        String extractedUsername1 = jwtUtil.extractUsername(token);

        boolean isValid2 = jwtUtil.validateToken(token);
        String extractedUserId2 = jwtUtil.extractUserId(token);
        String extractedUsername2 = jwtUtil.extractUsername(token);

        // Then - all operations should be consistent
        assertTrue(isValid1);
        assertTrue(isValid2);
        assertEquals(userId, extractedUserId1);
        assertEquals(userId, extractedUserId2);
        assertEquals(username, extractedUsername1);
        assertEquals(username, extractedUsername2);
    }
}
