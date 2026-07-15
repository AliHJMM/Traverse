package com.traverse.auth.security;

import com.traverse.auth.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}
