package com.traverse.travel.security;

import com.traverse.travel.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}
