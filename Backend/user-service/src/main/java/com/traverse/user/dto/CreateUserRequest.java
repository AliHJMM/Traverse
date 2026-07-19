package com.traverse.user.dto;

import com.traverse.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
        Role role,
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 50) String phone,
        @Size(max = 500) String address
) {
}
