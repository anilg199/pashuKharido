package com.pashumandi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pashumandi.dto.auth.LoginRequest;
import com.pashumandi.dto.auth.RefreshTokenRequest;
import com.pashumandi.dto.auth.RegisterRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

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

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user with default role BUYER and status ACTIVE")
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Anil")
                .lastName("Gupta")
                .phone("9876543210")
                .email("anil@example.com")
                .password("StrongPassword123")
                .state("Uttar Pradesh")
                .district("Kushinagar")
                .village("Example Village")
                .pincode("274001")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("User registered successfully")))
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
    @DisplayName("Should reject registration with duplicate phone number (HTTP 409)")
    void shouldRejectDuplicatePhone() throws Exception {
        User existing = User.builder()
                .firstName("Existing")
                .phone("9876543210")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Varanasi")
                .village("Village")
                .pincode("221001")
                .build();
        userRepository.save(existing);

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Another")
                .phone("9876543210")
                .password("Password123")
                .state("UP")
                .district("Lucknow")
                .village("Village")
                .pincode("226001")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("USER_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Should reject registration with duplicate email (HTTP 409)")
    void shouldRejectDuplicateEmail() throws Exception {
        User existing = User.builder()
                .firstName("Existing")
                .phone("9876543211")
                .email("duplicate@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Varanasi")
                .village("Village")
                .pincode("221001")
                .build();
        userRepository.save(existing);

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Another")
                .phone("9876543212")
                .email("duplicate@example.com")
                .password("Password123")
                .state("UP")
                .district("Lucknow")
                .village("Village")
                .pincode("226001")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("USER_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("Should reject invalid registration fields (HTTP 400)")
    void shouldRejectInvalidFields() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("") // empty
                .phone("123") // invalid phone
                .password("short") // less than 8 chars
                .state("")
                .district("")
                .village("")
                .pincode("invalid")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.validationErrors.firstName").exists())
                .andExpect(jsonPath("$.validationErrors.phone").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Should authenticate user via phone number and return JWT token pair")
    void shouldLoginWithPhone() throws Exception {
        User user = User.builder()
                .firstName("Ramesh")
                .phone("9876543210")
                .password(passwordEncoder.encode("SecretPass123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Gorakhpur")
                .village("Village")
                .pincode("273001")
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .identifier("9876543210")
                .password("SecretPass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.phone", is("9876543210")));
    }

    @Test
    @DisplayName("Should authenticate user via email and return JWT token pair")
    void shouldLoginWithEmail() throws Exception {
        User user = User.builder()
                .firstName("Suresh")
                .phone("9876543215")
                .email("suresh@example.com")
                .password(passwordEncoder.encode("SecretPass123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Gorakhpur")
                .village("Village")
                .pincode("273001")
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .identifier("suresh@example.com")
                .password("SecretPass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.email", is("suresh@example.com")));
    }

    @Test
    @DisplayName("Should reject login with wrong password (HTTP 401)")
    void shouldRejectWrongPassword() throws Exception {
        User user = User.builder()
                .firstName("Ramesh")
                .phone("9876543210")
                .password(passwordEncoder.encode("SecretPass123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Gorakhpur")
                .village("Village")
                .pincode("273001")
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .identifier("9876543210")
                .password("WrongPassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_CREDENTIALS")));
    }

    @Test
    @DisplayName("Should reject authentication for BLOCKED or SUSPENDED user (HTTP 403)")
    void shouldRejectBlockedUser() throws Exception {
        User user = User.builder()
                .firstName("Blocked")
                .phone("9876543210")
                .password(passwordEncoder.encode("SecretPass123"))
                .role(UserRole.BUYER)
                .status(UserStatus.BLOCKED)
                .state("UP")
                .district("Gorakhpur")
                .village("Village")
                .pincode("273001")
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .identifier("9876543210")
                .password("SecretPass123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCOUNT_NOT_ACTIVE")));
    }

    @Test
    @DisplayName("Should rotate refresh token and issue new access token")
    void shouldRefreshAndRotateToken() throws Exception {
        User user = User.builder()
                .firstName("TokenUser")
                .phone("9876543210")
                .password(passwordEncoder.encode("SecretPass123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Gorakhpur")
                .village("Village")
                .pincode("273001")
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .identifier("9876543210")
                .password("SecretPass123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseStr = loginResult.getResponse().getContentAsString();
        String initialRefreshToken = objectMapper.readTree(responseStr).path("data").path("refreshToken").asText();

        // Refresh Token Request
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(initialRefreshToken)
                .build();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        // Old refresh token must now be revoked and rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("INVALID_REFRESH_TOKEN")));
    }

    @Test
    @DisplayName("Should successfully logout and revoke refresh tokens")
    void shouldLogoutSuccessfully() throws Exception {
        User user = User.builder()
                .firstName("LogoutUser")
                .phone("9876543210")
                .password(passwordEncoder.encode("SecretPass123"))
                .role(UserRole.BUYER)
                .status(UserStatus.ACTIVE)
                .state("UP")
                .district("Gorakhpur")
                .village("Village")
                .pincode("273001")
                .build();
        User savedUser = userRepository.save(user);
        String token = jwtService.generateAccessToken(savedUser);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Logout successful")));
    }
}
