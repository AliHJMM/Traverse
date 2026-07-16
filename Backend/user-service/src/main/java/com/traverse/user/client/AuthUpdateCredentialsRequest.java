package com.traverse.user.client;

import com.traverse.user.entity.Role;

public record AuthUpdateCredentialsRequest(String email, Role role, Boolean enabled) {
}
