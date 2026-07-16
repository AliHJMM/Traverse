package com.traverse.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.user.client.AuthServiceClient;
import com.traverse.user.client.AuthUserResponse;
import com.traverse.user.dto.CreateUserRequest;
import com.traverse.user.dto.UpdateUserRequest;
import com.traverse.user.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class UserFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthServiceClient authServiceClient;

    @Test
    void adminCanCreateReadUpdateDeleteUser() throws Exception {
        when(authServiceClient.register(any())).thenReturn(new AuthUserResponse(101L, "traveler@example.com", Role.USER));
        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        CreateUserRequest createRequest = new CreateUserRequest(
                "traveler@example.com", "password123", Role.USER, "Jane Traveler", "555-1234", "1 Main St");
        mockMvc.perform(post("/api/users").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.fullName").value("Jane Traveler"));

        mockMvc.perform(get("/api/users/101").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("traveler@example.com"));

        when(authServiceClient.updateCredentials(eq(101L), any()))
                .thenReturn(new AuthUserResponse(101L, "traveler2@example.com", Role.ADMIN));
        UpdateUserRequest updateRequest = new UpdateUserRequest(
                "traveler2@example.com", Role.ADMIN, null, "Jane T.", null, null);
        mockMvc.perform(put("/api/users/101").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("traveler2@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.fullName").value("Jane T."));

        doNothing().when(authServiceClient).deleteCredentials(101L);
        mockMvc.perform(delete("/api/users/101").cookie(adminCookie)).andExpect(status().isNoContent());
        verify(authServiceClient).deleteCredentials(101L);

        mockMvc.perform(get("/api/users/101").cookie(adminCookie)).andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotAccessUserEndpoints() throws Exception {
        Cookie userCookie = tokenCookie(2L, "user@example.com", Role.USER);
        mockMvc.perform(get("/api/users").cookie(userCookie)).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailRejectedLocallyWithoutCallingAuthServiceAgain() throws Exception {
        when(authServiceClient.register(any())).thenReturn(new AuthUserResponse(202L, "dup@example.com", Role.USER));
        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        CreateUserRequest request = new CreateUserRequest("dup@example.com", "password123", Role.USER, "Dup User", null, null);
        mockMvc.perform(post("/api/users").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(authServiceClient, times(1)).register(any());
    }

    private Cookie tokenCookie(Long id, String email, Role role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject(String.valueOf(id))
                .claim("email", email)
                .claim("roles", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
        return new Cookie("access_token", token);
    }
}
