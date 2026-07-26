package com.traverse.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.travel.dto.CreateFeedbackRequest;
import com.traverse.travel.dto.CreateTravelRequest;
import com.traverse.travel.dto.DestinationRequest;
import com.traverse.travel.entity.Role;
import com.traverse.travel.service.DestinationGraphService;
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
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class StatsFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DestinationGraphService destinationGraphService;

    private Long createTravel(Cookie owner, BigDecimal price) throws Exception {
        CreateTravelRequest req = new CreateTravelRequest("Trip",
                LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusDays(5),
                List.of(new DestinationRequest("Paris", "France", null, null)), null, null, null, price);
        var res = mockMvc.perform(post("/api/travels").cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void managerAndTravelerAndAdminDashboardsAggregateCorrectly() throws Exception {
        Cookie manager = tokenCookie(10L, "manager@example.com", Role.TRAVEL_MANAGER);
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);
        Cookie admin = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        Long id = createTravel(manager, new BigDecimal("500.00"));
        mockMvc.perform(post("/api/travels/" + id + "/subscribe").cookie(traveler)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/travels/" + id + "/feedback").cookie(traveler)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateFeedbackRequest(4, "Good"))))
                .andExpect(status().isCreated());

        // Manager dashboard: 1 trip, 1 traveler, income 500, avg rating 4
        mockMvc.perform(get("/api/travels/stats/manager/me").cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripsCount").value(1))
                .andExpect(jsonPath("$.activeTravelersCount").value(1))
                .andExpect(jsonPath("$.totalIncome").value(500.00))
                .andExpect(jsonPath("$.averageRating").value(4.0));

        // Traveler dashboard: 1 active trip, 1 feedback given
        mockMvc.perform(get("/api/travels/stats/traveler/me").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTrips").value(1))
                .andExpect(jsonPath("$.feedbackGiven").value(1));

        // Admin overview: platform totals + leaderboards
        mockMvc.perform(get("/api/travels/stats/admin/overview").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTravels").value(1))
                .andExpect(jsonPath("$.totalActiveSubscriptions").value(1))
                .andExpect(jsonPath("$.totalIncome").value(500.00))
                .andExpect(jsonPath("$.topManagers[0].managerId").value(10))
                .andExpect(jsonPath("$.topTravels[0].subscribers").value(1))
                // 6-month income series, current month (last) holds this booking's income
                .andExpect(jsonPath("$.monthlyIncome.length()").value(6))
                .andExpect(jsonPath("$.monthlyIncome[5].income").value(500.00));

        // Public manager snapshot is visible to a traveler
        mockMvc.perform(get("/api/travels/stats/manager/10").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripsCount").value(1));
    }

    @Test
    void dashboardAccessIsRoleGuarded() throws Exception {
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);
        // Traveler cannot see the admin overview nor the manager-only dashboard
        mockMvc.perform(get("/api/travels/stats/admin/overview").cookie(traveler))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/travels/stats/manager/me").cookie(traveler))
                .andExpect(status().isForbidden());
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
