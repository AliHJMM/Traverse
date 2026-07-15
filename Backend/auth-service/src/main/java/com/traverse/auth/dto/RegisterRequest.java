package com.traverse.auth.dto;

import com.traverse.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        Role role
) {
}
