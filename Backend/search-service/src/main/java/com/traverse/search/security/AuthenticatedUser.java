package com.traverse.search.security;

import com.traverse.search.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}
