package com.traverse.auth.service;

import com.traverse.auth.entity.Role;
import com.traverse.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Test
    void tokenRoundTripsExpectedClaims() {
        JwtService jwtService = new JwtService(SECRET, 60);
        User user = new User("test@example.com", "hash", Role.ADMIN);
        user.setId(7L);

        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
        assertThat(claims.get("roles", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void expiredTokenFailsToParse() {
        JwtService jwtService = new JwtService(SECRET, -1);
        User user = new User("test@example.com", "hash", Role.USER);
        user.setId(1L);

        String token = jwtService.generateToken(user);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
