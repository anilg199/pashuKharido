package com.pashumandi.service;

import com.pashumandi.dto.user.ChangePasswordRequest;
import com.pashumandi.dto.user.UpdateProfileRequest;
import com.pashumandi.dto.user.UserProfileResponse;
import com.pashumandi.entity.User;
import com.pashumandi.exception.InvalidPasswordException;
import com.pashumandi.exception.ResourceNotFoundException;
import com.pashumandi.exception.UserAlreadyExistsException;
import com.pashumandi.repository.RefreshTokenRepository;
import com.pashumandi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(Long userId) {
        User user = findUserById(userId);
        return UserProfileResponse.fromEntity(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        // Validate email uniqueness if changing email
        String newEmail = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim().toLowerCase() : null;
        if (newEmail != null && !newEmail.equalsIgnoreCase(user.getEmail())) {
            Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(userId)) {
                throw new UserAlreadyExistsException("Email " + newEmail + " is already in use by another account");
            }
            user.setEmail(newEmail);
        } else if (newEmail == null) {
            user.setEmail(null);
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(StringUtils.hasText(request.getLastName()) ? request.getLastName().trim() : null);

        if (StringUtils.hasText(request.getProfileImageUrl())) {
            user.setProfileImageUrl(request.getProfileImageUrl().trim());
        }

        user.setState(request.getState().trim());
        user.setDistrict(request.getDistrict().trim());
        user.setVillage(request.getVillage().trim());
        user.setPincode(request.getPincode().trim());

        // Explicitly ensuring id, role, status, and password cannot be mutated via this endpoint
        User updatedUser = userRepository.save(user);
        log.info("Profile successfully updated for user ID: {}", userId);

        return UserProfileResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password provided does not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password cannot be identical to the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Security best practice: invalidate existing refresh tokens to invalidate other active sessions
        refreshTokenRepository.revokeAllTokensForUser(userId);
        log.info("Password changed and active sessions revoked for user ID: {}", userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
