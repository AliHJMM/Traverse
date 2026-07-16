package com.traverse.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.auth.dto.LoginRequest;
import com.traverse.auth.dto.RegisterRequest;
import com.traverse.auth.dto.UpdateCredentialsRequest;
import com.traverse.auth.entity.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginMeLogoutFlow() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("admin@example.com", "password123", null));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN")); // first ever account bootstraps as ADMIN

        String loginBody = objectMapper.writeValueAsString(new LoginRequest("admin@example.com", "password123"));
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("SameSite=Strict");
        // the JWT itself must never appear in the JSON body -- only in the httpOnly cookie
        assertThat(loginResult.getResponse().getContentAsString()).doesNotContain("eyJ");

        Cookie authCookie = loginResult.getResponse().getCookie("access_token");
        assertThat(authCookie).isNotNull();

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(post("/api/auth/logout").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("access_token", 0));
    }

    @Test
    void publicRegistrationCannotSelfAssignAdminAfterBootstrap() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("first@example.com", "password123", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("second@example.com", "password123", Role.ADMIN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void duplicateEmailRegistrationRejected() throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterRequest("dup@example.com", "password123", null));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("wrongpass@example.com", "password123", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("wrongpass@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("short@example.com", "short", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanUpdateAndDeleteAnotherUsersCredentials() throws Exception {
        Cookie adminCookie = registerAndLogin("admin2@example.com", "password123");
        Long targetId = registerAndGetId("target@example.com", "password123");

        UpdateCredentialsRequest patch = new UpdateCredentialsRequest("target-renamed@example.com", Role.ADMIN, false);
        mockMvc.perform(patch("/api/auth/users/" + targetId).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("target-renamed@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // disabled account can no longer log in
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("target-renamed@example.com", "password123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/auth/users/" + targetId).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/auth/users/" + targetId).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotManageOtherUsers() throws Exception {
        Long targetId = registerAndGetId("first-becomes-admin@example.com", "password123");
        Cookie regularUserCookie = registerAndLogin("plain-user@example.com", "password123");

        mockMvc.perform(delete("/api/auth/users/" + targetId).cookie(regularUserCookie))
                .andExpect(status().isForbidden());
    }

    private Long registerAndGetId(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Cookie registerAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password, null))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return loginResult.getResponse().getCookie("access_token");
    }
}
