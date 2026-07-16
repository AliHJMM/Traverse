package com.traverse.auth.controller;

import com.traverse.auth.dto.LoginRequest;
import com.traverse.auth.dto.RegisterRequest;
import com.traverse.auth.dto.UpdateCredentialsRequest;
import com.traverse.auth.dto.UserResponse;
import com.traverse.auth.entity.Role;
import com.traverse.auth.entity.User;
import com.traverse.auth.security.AuthenticatedUser;
import com.traverse.auth.service.AuthService;
import com.traverse.auth.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final String cookieName;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, JwtService jwtService,
                           @Value("${app.cookie.name}") String cookieName,
                           @Value("${app.cookie.secure}") boolean cookieSecure) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request, currentCallerRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticate(request);
        String token = jwtService.generateToken(user);
        ResponseCookie cookie = buildCookie(token, jwtService.getExpirationSeconds());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(toResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = buildCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new UserResponse(principal.id(), principal.email(), principal.role()));
    }

    /**
     * Internal, ADMIN-only endpoints used by User Service to keep the
     * credential record (email/role/enabled) in sync when an admin edits or
     * removes a user's profile. Not meant to be called directly by the
     * frontend -- it goes through User Service, which owns the rest of the
     * user's profile data.
     */
    @PatchMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateCredentials(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateCredentialsRequest request) {
        User user = authService.updateCredentials(id, request);
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie buildCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private Role currentCallerRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.role();
        }
        return null;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
