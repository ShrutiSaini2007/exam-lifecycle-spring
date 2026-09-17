package com.examlifecycle.dto;

import com.examlifecycle.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") @Size(min = 8, message = "password must be at least 8 characters") String password,
            @NotNull(message = "role is required") Role role) {}

    public record LoginRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "password is required") String password) {}

    public record UserView(Long id, String username, Role role, Instant createdAt) {}

    public record TokenResponse(String token, UserView user) {}
}
