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
    void travelerCanCreateListAndDeleteTheirOwnStripeMethod() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);
        // Owner is taken from the authenticated principal (id 2), not the body.
        when(stripeClient.attach(2L, "pm_test_123"))
                .thenReturn(new AttachedPaymentMethod("pm_test_123", "visa", "4242", 12, 2030, null));

        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);

        CreatePaymentMethodRequest createRequest =
                new CreatePaymentMethodRequest(PaymentProvider.STRIPE, "pm_test_123", true);
        var createResult = mockMvc.perform(post("/api/payments").cookie(traveler)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("STRIPE"))
                .andExpect(jsonPath("$.brand").value("visa"))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // The traveler sees their own method with no userId param.
        mockMvc.perform(get("/api/payments").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        doNothing().when(stripeClient).detach("pm_test_123");
        mockMvc.perform(delete("/api/payments/" + id).cookie(traveler))
                .andExpect(status().isNoContent());
        verify(stripeClient).detach("pm_test_123");

        mockMvc.perform(get("/api/payments/" + id).cookie(traveler))
                .andExpect(status().isNotFound());
    }

    @Test
    void travelersCannotSeeEachOthersMethods() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);
        when(stripeClient.attach(2L, "pm_owned"))
                .thenReturn(new AttachedPaymentMethod("pm_owned", "visa", "4242", 12, 2030, null));

        Cookie owner = tokenCookie(2L, "a@example.com", Role.TRAVELER);
        Cookie other = tokenCookie(3L, "b@example.com", Role.TRAVELER);

        var res = mockMvc.perform(post("/api/payments").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePaymentMethodRequest(PaymentProvider.STRIPE, "pm_owned", true))))
                .andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Another traveler sees nothing and cannot fetch or delete the method.
        mockMvc.perform(get("/api/payments").cookie(other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/payments/" + id).cookie(other)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/payments/" + id).cookie(other)).andExpect(status().isNotFound());
    }

    @Test
    void settingNewDefaultUnsetsThePreviousOne() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);
        when(stripeClient.attach(1L, "pm_first"))
                .thenReturn(new AttachedPaymentMethod("pm_first", "visa", "1111", 1, 2030, null));
        when(stripeClient.attach(1L, "pm_second"))
                .thenReturn(new AttachedPaymentMethod("pm_second", "mastercard", "2222", 2, 2031, null));

        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        mockMvc.perform(post("/api/payments").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePaymentMethodRequest(PaymentProvider.STRIPE, "pm_first", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(post("/api/payments").cookie(adminCookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePaymentMethodRequest(PaymentProvider.STRIPE, "pm_second", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        String listJson = mockMvc.perform(get("/api/payments").param("userId", "1").cookie(adminCookie))
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
        when(stripeClient.attach(7L, "pm_invalid"))
                .thenThrow(new PaymentGatewayException("Stripe rejected payment method pm_invalid"));

        Cookie traveler = tokenCookie(7L, "t@example.com", Role.TRAVELER);
        mockMvc.perform(post("/api/payments").cookie(traveler).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePaymentMethodRequest(PaymentProvider.STRIPE, "pm_invalid", false))))
                .andExpect(status().isBadGateway());
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
