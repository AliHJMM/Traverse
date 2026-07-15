package com.traverse.user.dto;

import com.traverse.user.entity.Role;
import jakarta.validation.constraints.Email;

public record UpdateUserRequest(
        @Email String email,
        Role role,
        Boolean enabled,
        String fullName,
        String phone,
        String address
) {
}
