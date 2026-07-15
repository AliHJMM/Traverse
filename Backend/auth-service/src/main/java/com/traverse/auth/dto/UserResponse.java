package com.traverse.auth.dto;

import com.traverse.auth.entity.Role;

public record UserResponse(Long id, String email, Role role) {
}
