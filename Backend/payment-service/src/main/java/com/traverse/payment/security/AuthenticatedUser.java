package com.traverse.payment.security;

import com.traverse.payment.entity.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}
