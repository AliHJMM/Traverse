package com.traverse.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.payment.dto.CreatePaymentMethodRequest;
import com.traverse.payment.entity.PaymentProvider;
import com.traverse.payment.entity.Role;
import com.traverse.payment.gateway.AttachedPaymentMethod;
import com.traverse.payment.gateway.PaymentGatewayException;
import com.traverse.payment.gateway.PaypalPaymentGatewayClient;
import com.traverse.payment.gateway.StripePaymentGatewayClient;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class PaymentMethodFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StripePaymentGatewayClient stripeClient;

    @MockBean
    private PaypalPaymentGatewayClient paypalClient;

    @Test
    void adminCanCreateListAndDeleteAStripePaymentMethod() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(stripeClient.attach(42L, "pm_test_123"))
                .thenReturn(new AttachedPaymentMethod("pm_test_123", "visa", "4242", 12, 2030, null));
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);

        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        CreatePaymentMethodRequest createRequest = new CreatePaymentMethodRequest(42L, PaymentProvider.STRIPE, "pm_test_123", true);
        var createResult = mockMvc.perform(post("/api/payments").cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("STRIPE"))
                .andExpect(jsonPath("$.brand").value("visa"))
                .andExpect(jsonPath("$.last4").value("4242"))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/payments").param("userId", "42").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        doNothing().when(stripeClient).detach("pm_test_123");
        mockMvc.perform(delete("/api/payments/" + id).cookie(adminCookie))
                .andExpect(status().isNoContent());
        verify(stripeClient).detach("pm_test_123");

        mockMvc.perform(get("/api/payments/" + id).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void settingNewDefaultUnsetsThePreviousOne() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);
        when(stripeClient.attach(7L, "pm_first"))
                .thenReturn(new AttachedPaymentMethod("pm_first", "visa", "1111", 1, 2030, null));
        when(stripeClient.attach(7L, "pm_second"))
                .thenReturn(new AttachedPaymentMethod("pm_second", "mastercard", "2222", 2, 2031, null));

        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/payments").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePaymentMethodRequest(7L, PaymentProvider.STRIPE, "pm_first", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(post("/api/payments").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePaymentMethodRequest(7L, PaymentProvider.STRIPE, "pm_second", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        String listJson = mockMvc.perform(get("/api/payments").param("userId", "7").cookie(adminCookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var defaults = objectMapper.readTree(listJson);
        int defaultCount = 0;
        for (var node : defaults) {
            if (node.get("isDefault").asBoolean()) {
                defaultCount++;
            }
        }
        org.assertj.core.api.Assertions.assertThat(defaultCount).isEqualTo(1);
    }

    @Test
    void gatewayRejectionSurfacesAsBadGateway() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);
        when(stripeClient.attach(7L, "pm_invalid")).thenThrow(new PaymentGatewayException("Stripe rejected payment method pm_invalid"));

        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        mockMvc.perform(post("/api/payments").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePaymentMethodRequest(7L, PaymentProvider.STRIPE, "pm_invalid", false))))
                .andExpect(status().isBadGateway());
    }

    @Test
    void nonAdminCannotAccessPaymentEndpoints() throws Exception {
        Cookie userCookie = tokenCookie(2L, "user@example.com", Role.USER);
        mockMvc.perform(get("/api/payments").cookie(userCookie)).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/payments")).andExpect(status().isUnauthorized());
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
