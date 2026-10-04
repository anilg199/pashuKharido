package com.pashumandi.service;

import com.pashumandi.dto.auth.AuthResponse;
import com.pashumandi.dto.auth.LoginRequest;
import com.pashumandi.dto.auth.RefreshTokenRequest;
import com.pashumandi.dto.auth.RegisterRequest;
import com.pashumandi.dto.user.UserProfileResponse;
import com.pashumandi.entity.RefreshToken;
import com.pashumandi.entity.User;
import com.pashumandi.enums.UserRole;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.exception.AccountStatusException;
import com.pashumandi.exception.InvalidCredentialsException;
import com.pashumandi.exception.TokenRefreshException;
import com.pashumandi.exception.UserAlreadyExistsException;
import com.pashumandi.repository.RefreshTokenRepository;
import com.pashumandi.repository.UserRepository;
import com.pashumandi.security.JwtService;
import com.pashumandi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String phone = request.getPhone().trim();
        if (userRepository.existsByPhone(phone)) {
            throw new UserAlreadyExistsException("Phone number " + phone + " is already registered");
        }

        String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim().toLowerCase() : null;
        if (email != null && userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email " + email + " is already registered");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : null)
                .phone(phone)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.BUYER) // Default role is strictly BUYER
                .status(UserStatus.ACTIVE) // Default status is ACTIVE
                .state(request.getState().trim())
                .district(request.getDistrict().trim())
                .village(request.getVillage().trim())
                .pincode(request.getPincode().trim())
                .lastLoginAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered successfully with ID: {} and phone: {}", savedUser.getId(), savedUser.getPhone());

        String accessToken = jwtService.generateAccessToken(savedUser);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(UserProfileResponse.fromEntity(savedUser))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        User user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid phone/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password attempt for identifier: {}", identifier);
            throw new InvalidCredentialsException("Invalid phone/email or password");
        }

        validateUserStatus(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        log.info("User {} successfully authenticated", user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(UserProfileResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenStr = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new TokenRefreshException("Refresh token was not found in database"));

        if (refreshToken.isRevoked()) {
            log.warn("Revoked refresh token reuse attempt detected: {}", tokenStr);
            // Security measure: Revoke all tokens for this user upon detecting reused/compromised token
            refreshTokenRepository.revokeAllTokensForUser(refreshToken.getUser().getId());
            throw new TokenRefreshException("Refresh token has been revoked. All sessions invalidated.");
        }

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new TokenRefreshException("Refresh token has expired. Please login again.");
        }

        User user = refreshToken.getUser();
        validateUserStatus(user);

        // Refresh Token Rotation: Revoke current refresh token and generate a new one
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        RefreshToken newRefreshToken = createRefreshToken(user);
        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationSeconds())
                .user(UserProfileResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public void logout(UserPrincipal principal) {
        if (principal != null && principal.getId() != null) {
            refreshTokenRepository.revokeAllTokensForUser(principal.getId());
            log.info("All refresh tokens revoked for user ID: {}", principal.getId());
        }
    }

    private RefreshToken createRefreshToken(User user) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtService.getRefreshExpirationMillis() / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountStatusException("Your account has been blocked. Please contact customer support.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountStatusException("Your account is currently suspended. Please contact customer support.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("Your account is inactive. Please activate your account.");
        }
    }
}
