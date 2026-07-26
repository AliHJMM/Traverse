package com.traverse.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.travel.dto.CreateFeedbackRequest;
import com.traverse.travel.dto.CreateReportRequest;
import com.traverse.travel.dto.CreateTravelRequest;
import com.traverse.travel.dto.DestinationRequest;
import com.traverse.travel.entity.ReportSubjectType;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class FeedbackReportFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DestinationGraphService destinationGraphService;

    private Long createTravel(Cookie owner) throws Exception {
        CreateTravelRequest req = new CreateTravelRequest("Trip",
                LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusDays(5),
                List.of(new DestinationRequest("Paris", "France", null, null)), null, null, null, null);
        var res = mockMvc.perform(post("/api/travels").cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void feedbackRequiresSubscriptionThenSucceeds() throws Exception {
        Cookie admin = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);
        Long id = createTravel(admin);

        String fb = objectMapper.writeValueAsString(new CreateFeedbackRequest(5, "Amazing trip"));

        // not subscribed -> cannot review
        mockMvc.perform(post("/api/travels/" + id + "/feedback").cookie(traveler)
                        .contentType(MediaType.APPLICATION_JSON).content(fb))
                .andExpect(status().isConflict());

        // subscribe, then review
        mockMvc.perform(post("/api/travels/" + id + "/subscribe").cookie(traveler)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/travels/" + id + "/feedback").cookie(traveler)
                        .contentType(MediaType.APPLICATION_JSON).content(fb))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
        verify(destinationGraphService).recordRating(2L, id, 5);

        // feedback visible on the travel and in "mine"
        mockMvc.perform(get("/api/travels/" + id + "/feedback").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comment").value("Amazing trip"));
        mockMvc.perform(get("/api/travels/feedback/mine").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void reportFilingAndAdminReviewFlow() throws Exception {
        Cookie admin = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);

        String report = objectMapper.writeValueAsString(
                new CreateReportRequest(ReportSubjectType.MANAGER, 10L, null, "Rude behavior"));
        var res = mockMvc.perform(post("/api/travels/reports").cookie(traveler)
                        .contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        long reportId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // reporter sees their own report
        mockMvc.perform(get("/api/travels/reports/mine").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // non-admin cannot list all reports
        mockMvc.perform(get("/api/travels/reports").cookie(traveler)).andExpect(status().isForbidden());

        // admin reviews all + marks reviewed
        mockMvc.perform(get("/api/travels/reports").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Rude behavior"));
        mockMvc.perform(patch("/api/travels/reports/" + reportId + "/review").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWED"));
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
