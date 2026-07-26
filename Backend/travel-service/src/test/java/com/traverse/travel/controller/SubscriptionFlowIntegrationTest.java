package com.traverse.travel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class SubscriptionFlowIntegrationTest {

    private static final String SECRET = "test-secret-key-please-be-at-least-32-bytes-long";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DestinationGraphService destinationGraphService;

    private Long createTravel(Cookie owner, LocalDate start, LocalDate end) throws Exception {
        CreateTravelRequest req = new CreateTravelRequest("Trip", start, end,
                List.of(new DestinationRequest("Paris", "France", null, null)), null, null, null);
        var res = mockMvc.perform(post("/api/travels").cookie(owner)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void travelerSubscribeUnsubscribeFlow() throws Exception {
        Cookie admin = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);
        Long id = createTravel(admin, LocalDate.now().plusMonths(2), LocalDate.now().plusMonths(2).plusDays(5));

        // subscribe
        mockMvc.perform(post("/api/travels/" + id + "/subscribe").cookie(traveler))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.travelId").value(id))
                .andExpect(jsonPath("$.status").value("SUBSCRIBED"));
        verify(destinationGraphService).recordParticipation(2L, id);

        // duplicate subscribe -> 409
        mockMvc.perform(post("/api/travels/" + id + "/subscribe").cookie(traveler))
                .andExpect(status().isConflict());

        // appears in my subscriptions
        mockMvc.perform(get("/api/travels/subscriptions/mine").cookie(traveler))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // unsubscribe
        mockMvc.perform(delete("/api/travels/" + id + "/subscribe").cookie(traveler))
                .andExpect(status().isNoContent());
        verify(destinationGraphService).removeParticipation(2L, id);

        // unsubscribe again -> 409 (not subscribed)
        mockMvc.perform(delete("/api/travels/" + id + "/subscribe").cookie(traveler))
                .andExpect(status().isConflict());
    }

    @Test
    void subscribeWithinCutoffIsRejected() throws Exception {
        Cookie admin = tokenCookie(1L, "admin@example.com", Role.ADMIN);
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);
        // departs in 1 day -> inside the 3-day cutoff
        Long id = createTravel(admin, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        mockMvc.perform(post("/api/travels/" + id + "/subscribe").cookie(traveler))
                .andExpect(status().isConflict());
    }

    @Test
    void managerSeesOwnSubscribersOnly() throws Exception {
        Cookie managerA = tokenCookie(10L, "a@example.com", Role.TRAVEL_MANAGER);
        Cookie managerB = tokenCookie(11L, "b@example.com", Role.TRAVEL_MANAGER);
        Cookie traveler = tokenCookie(2L, "traveler@example.com", Role.TRAVELER);
        Long id = createTravel(managerA, LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(1).plusDays(4));

        mockMvc.perform(post("/api/travels/" + id + "/subscribe").cookie(traveler))
                .andExpect(status().isCreated());

        // owner sees the subscriber
        mockMvc.perform(get("/api/travels/" + id + "/subscribers").cookie(managerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].travelerId").value(2));

        // another manager cannot
        mockMvc.perform(get("/api/travels/" + id + "/subscribers").cookie(managerB))
                .andExpect(status().isForbidden());

        // owner removes the subscriber
        mockMvc.perform(delete("/api/travels/" + id + "/subscribers/2").cookie(managerA))
                .andExpect(status().isNoContent());
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
