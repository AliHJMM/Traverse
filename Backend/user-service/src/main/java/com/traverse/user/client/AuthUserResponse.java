package com.traverse.user.client;

import com.traverse.user.entity.Role;

public record AuthUserResponse(Long id, String email, Role role) {
}
