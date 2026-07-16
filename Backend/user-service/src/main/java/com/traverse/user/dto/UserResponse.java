package com.traverse.user.dto;

import com.traverse.user.entity.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        Role role,
        String fullName,
        String phone,
        String address,
        boolean enabled,
        Instant createdAt
) {
}
