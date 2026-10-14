package com.pashumandi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pashumandi.dto.user.ChangePasswordRequest;
import com.pashumandi.dto.user.UpdateProfileRequest;
import com.pashumandi.entity.RefreshToken;
import com.pashumandi.entity.User;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.repository.RefreshTokenRepository;
import com.pashumandi.repository.UserRepository;
import com.pashumandi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .firstName("Anil")
                .lastName("Gupta")
                .phone("9876543210")
                .email("anil@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("Uttar Pradesh")
                .district("Kushinagar")
                .village("Example Village")
                .pincode("274001")
                .build();
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateAccessToken(testUser);
    }

    @Test
    @DisplayName("Should return user profile and NEVER expose password fields")
    void shouldReturnUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(testUser.getId().intValue())))
                .andExpect(jsonPath("$.data.firstName", is("Anil")))
                .andExpect(jsonPath("$.data.lastName", is("Gupta")))
                .andExpect(jsonPath("$.data.phone", is("9876543210")))
                .andExpect(jsonPath("$.data.email", is("anil@example.com")))
                .andExpect(jsonPath("$.data.role", is("BUYER")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should reject unauthorized request without JWT token (HTTP 401)")
    void shouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Should reject request with invalid JWT token (HTTP 401)")
    void shouldRejectInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void shouldUpdateProfileSuccessfully() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Anil Kumar")
                .lastName("Gupta")
                .email("anil.updated@example.com")
                .state("Uttar Pradesh")
                .district("Gorakhpur")
                .village("New Village")
                .pincode("273001")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.firstName", is("Anil Kumar")))
                .andExpect(jsonPath("$.data.email", is("anil.updated@example.com")))
                .andExpect(jsonPath("$.data.district", is("Gorakhpur")))
                .andExpect(jsonPath("$.data.pincode", is("273001")));
    }

    @Test
    @DisplayName("Should change password and invalidate all active refresh tokens")
    void shouldChangePasswordAndRevokeTokens() throws Exception {
        // Create an active refresh token for this user
        RefreshToken token = RefreshToken.builder()
                .token("session-refresh-token-12345")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Password123")
                .newPassword("NewSuperSecret456")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // Verify tokens are now revoked in database
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(testUser);
        assertTrue(activeTokens.isEmpty());
    }

    @Test
    @DisplayName("Should reject password change if current password is wrong (HTTP 400)")
    void shouldRejectPasswordChangeWithWrongCurrentPassword() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword123")
                .newPassword("NewSuperSecret456")
                .build();

        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_PASSWORD")));
    }
}
