package com.pashumandi.security;

import com.pashumandi.entity.User;
import com.pashumandi.enums.UserStatus;
import com.pashumandi.exception.AccountStatusException;
import com.pashumandi.exception.InvalidCredentialsException;
import com.pashumandi.exception.ResourceNotFoundException;
import com.pashumandi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid phone/email or password"));

        validateUserStatus(user);

        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        validateUserStatus(user);

        return UserPrincipal.create(user);
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountStatusException("Your account has been blocked. Please contact support.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AccountStatusException("Your account is currently suspended. Please contact support.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("Your account is inactive. Please activate your account to proceed.");
        }
    }
}
