package com.traverse.user.dto;

import com.traverse.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 255) String email,
        Role role,
        Boolean enabled,
        @Size(max = 255) String fullName,
        @Size(max = 50) String phone,
        @Size(max = 500) String address
) {
}
