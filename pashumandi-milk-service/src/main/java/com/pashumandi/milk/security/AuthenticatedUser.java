package com.pashumandi.milk.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
@Builder
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || authorities.stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    public boolean isSeller() {
        return "SELLER".equalsIgnoreCase(role) || authorities.stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SELLER"));
    }

    public boolean isBuyer() {
        return "BUYER".equalsIgnoreCase(role) || authorities.stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_BUYER"));
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public String getPassword() {
        return ""; // No passwords in stateless microservice
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
