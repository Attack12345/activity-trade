package com.example.activitytrade.security.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtUtilTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 120);

    @Test
    void roundTripKeepsClaims() {
        String token = jwtUtil.generate(1001L, "alice", 0);
        Claims claims = jwtUtil.parse(token);
        assertNotNull(claims);
        assertEquals("1001", claims.getSubject());
        assertEquals("alice", claims.get("username", String.class));
        assertEquals(0, claims.get("role", Integer.class));
    }

    @Test
    void invalidTokenReturnsNull() {
        assertNull(jwtUtil.parse("not-a-jwt-token"));
    }

    @Test
    void expiredTokenReturnsNull() {
        JwtUtil expired = new JwtUtil(SECRET, 0);
        String token = expired.generate(1L, "a", 0);
        assertNull(expired.parse(token));
    }

    @Test
    void userIdOfExtractsSubject() {
        Claims claims = jwtUtil.parse(jwtUtil.generate(5L, "b", 1));
        assertEquals(5L, JwtUtil.userIdOf(claims));
    }
}