package com.traverse.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.payment.dto.ChargeRequest;
import com.traverse.payment.dto.CreatePaymentMethodRequest;
import com.traverse.payment.entity.PaymentProvider;
import com.traverse.payment.entity.Role;
import com.traverse.payment.gateway.AttachedPaymentMethod;
import com.traverse.payment.gateway.PaymentGatewayException;
import com.traverse.payment.gateway.PaypalPaymentGatewayClient;
import com.traverse.payment.gateway.StripePaymentGatewayClient;
import com.traverse.payment.service.TravelPricingClient;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class PaymentChargeFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private StripePaymentGatewayClient stripeClient;
    @MockBean
    private PaypalPaymentGatewayClient paypalClient;
    @MockBean
    private TravelPricingClient travelPricingClient;

    private long createMethod(Cookie owner, long ownerId, String token) throws Exception {
        when(stripeClient.attach(ownerId, token))
                .thenReturn(new AttachedPaymentMethod(token, "visa", "4242", 12, 2030, null));
        var res = mockMvc.perform(post("/api/payments").cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePaymentMethodRequest(PaymentProvider.STRIPE, token, true))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void travelerPaysForABookingAndSeesItInHistory() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);

        Cookie traveler = tokenCookie(2L, "t@example.com", Role.TRAVELER);
        long methodId = createMethod(traveler, 2L, "pm_ok");
        // Authoritative price comes from travel-service, not the request.
        when(travelPricingClient.priceOf(100L)).thenReturn(new BigDecimal("500.00"));
        when(stripeClient.charge(2L, "pm_ok", 50000L, "USD")).thenReturn("pi_123");

        mockMvc.perform(post("/api/payments/charges").cookie(traveler).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChargeRequest(100L, methodId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.travelId").value(100))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.externalChargeId").value("pi_123"));

        mockMvc.perform(get("/api/payments/charges/mine").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
    }

    @Test
    void cannotPayWithSomeoneElsesMethod() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);

        Cookie owner = tokenCookie(2L, "a@example.com", Role.TRAVELER);
        Cookie attacker = tokenCookie(3L, "b@example.com", Role.TRAVELER);
        long methodId = createMethod(owner, 2L, "pm_owned");

        mockMvc.perform(post("/api/payments/charges").cookie(attacker).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChargeRequest(100L, methodId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void declinedChargeRecordsAFailedPayment() throws Exception {
        when(stripeClient.provider()).thenReturn(PaymentProvider.STRIPE);
        when(paypalClient.provider()).thenReturn(PaymentProvider.PAYPAL);

        Cookie traveler = tokenCookie(2L, "t@example.com", Role.TRAVELER);
        long methodId = createMethod(traveler, 2L, "pm_decline");
        when(travelPricingClient.priceOf(100L)).thenReturn(new BigDecimal("500.00"));
        when(stripeClient.charge(2L, "pm_decline", 50000L, "USD"))
                .thenThrow(new PaymentGatewayException("card declined"));

        mockMvc.perform(post("/api/payments/charges").cookie(traveler).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChargeRequest(100L, methodId))))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"));

        // The failed attempt is still recorded in the ledger.
        mockMvc.perform(get("/api/payments/charges/mine").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("FAILED"));
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
