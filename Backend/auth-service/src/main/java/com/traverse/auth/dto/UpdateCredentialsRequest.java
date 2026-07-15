package com.traverse.auth.dto;

import com.traverse.auth.entity.Role;
import jakarta.validation.constraints.Email;

public record UpdateCredentialsRequest(
        @Email String email,
        Role role,
        Boolean enabled
) {
}
