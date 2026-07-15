package com.traverse.user.client;

import com.traverse.user.entity.Role;

public record AuthRegisterRequest(String email, String password, Role role) {
}
