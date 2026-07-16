package com.traverse.user.security;

import com.traverse.user.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}
