package com.pashumandi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Phone number or email identifier is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
