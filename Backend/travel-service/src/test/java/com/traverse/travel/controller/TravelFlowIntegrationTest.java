package com.traverse.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traverse.travel.dto.AccommodationRequest;
import com.traverse.travel.dto.ActivityRequest;
import com.traverse.travel.dto.CreateTravelRequest;
import com.traverse.travel.dto.DestinationRequest;
import com.traverse.travel.dto.NearbyDestinationResponse;
import com.traverse.travel.dto.TransportationRequest;
import com.traverse.travel.dto.UpdateTravelRequest;
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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
class TravelFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DestinationGraphService destinationGraphService;

    @Test
    void adminCanCreateReadUpdateDeleteTravel() throws Exception {
        doNothing().when(destinationGraphService).syncItinerary(any());
        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);

        CreateTravelRequest createRequest = new CreateTravelRequest(
                "Europe Trip", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                List.of(new DestinationRequest("Paris", "France", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5)),
                        new DestinationRequest("Rome", "Italy", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 10))),
                List.of(new ActivityRequest("Louvre tour", "Museum visit", "Paris", LocalDate.of(2026, 8, 2), new BigDecimal("25.00"))),
                List.of(new AccommodationRequest("Hotel Lumiere", "Hotel", "1 Rue de Paris",
                        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5))),
                List.of(new TransportationRequest("Flight", "AirFrance", "Paris", "Rome",
                        LocalDateTime.of(2026, 8, 5, 9, 0), LocalDateTime.of(2026, 8, 5, 11, 0))));

        String createBody = objectMapper.writeValueAsString(createRequest);
        var createResult = mockMvc.perform(post("/api/travels").cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Europe Trip"))
                .andExpect(jsonPath("$.durationDays").value(10))
                .andExpect(jsonPath("$.destinations.length()").value(2))
                .andExpect(jsonPath("$.activities.length()").value(1))
                .andExpect(jsonPath("$.accommodations.length()").value(1))
                .andExpect(jsonPath("$.transportations.length()").value(1))
                .andReturn();

        verify(destinationGraphService).syncItinerary(createRequest.destinations());

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/travels/" + id).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Europe Trip"));

        UpdateTravelRequest updateRequest = new UpdateTravelRequest(
                "Europe Trip Extended", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 12),
                List.of(new DestinationRequest("Paris", "France", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5))),
                null, null, null);
        mockMvc.perform(put("/api/travels/" + id).cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Europe Trip Extended"))
                .andExpect(jsonPath("$.durationDays").value(12))
                .andExpect(jsonPath("$.destinations.length()").value(1))
                .andExpect(jsonPath("$.activities.length()").value(0));

        mockMvc.perform(delete("/api/travels/" + id).cookie(adminCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/travels/" + id).cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminCannotAccessTravelEndpoints() throws Exception {
        Cookie userCookie = tokenCookie(2L, "user@example.com", Role.USER);
        mockMvc.perform(get("/api/travels").cookie(userCookie)).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/travels")).andExpect(status().isUnauthorized());
    }

    @Test
    void creationRequiresAtLeastOneDestination() throws Exception {
        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        CreateTravelRequest invalid = new CreateTravelRequest(
                "No Destinations", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                List.of(), null, null, null);

        mockMvc.perform(post("/api/travels").cookie(adminCookie)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyEndpointDelegatesToGraphService() throws Exception {
        Cookie adminCookie = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        when(destinationGraphService.findNearby("Paris"))
                .thenReturn(List.of(new NearbyDestinationResponse("Rome", "Italy")));

        mockMvc.perform(get("/api/travels/destinations/Paris/nearby").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].city").value("Rome"))
                .andExpect(jsonPath("$[0].country").value("Italy"));
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
