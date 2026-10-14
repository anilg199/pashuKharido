package com.pashumandi.security;

import com.pashumandi.config.JwtConfig;
import com.pashumandi.entity.User;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        // 256-bit Base64-encoded test secret
        jwtConfig.setSecret("dGVzdF9zZWNyZXRfa2V5X2Zvcl9wYXNodW1hbmRpX2F1dGhlbnRpY2F0aW9uX3N5c3RlbV8yNTZiaXRzIQ==");
        jwtConfig.setAccessExpiration(900000); // 15 mins
        jwtConfig.setRefreshExpiration(604800000); // 7 days

        jwtService = new JwtService(jwtConfig);
        jwtService.init();
    }

    @Test
    @DisplayName("Should generate valid token and extract user claims")
    void shouldGenerateAndExtractClaims() {
        User user = User.builder()
                .id(42L)
                .phone("9876543210")
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("BUYER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should reject invalid and malformed tokens")
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.validateToken("this.is.an.invalid.token"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    @DisplayName("Should identify expired tokens")
    void shouldIdentifyExpiredTokens() {
        jwtConfig.setAccessExpiration(-10000); // 10 seconds in the past
        JwtService expiredJwtService = new JwtService(jwtConfig);
        expiredJwtService.init();

        String token = expiredJwtService.generateAccessToken(1L, "BUYER");
        assertNotNull(token);
        assertFalse(expiredJwtService.validateToken(token));
    }
}
