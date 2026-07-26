package com.traverse.travel.service;

import com.traverse.travel.entity.Travel;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps the Elasticsearch index (owned by search-service) in sync with the
 * Postgres source of truth. Every call is best-effort: search is a secondary
 * read model, so a transient search-service/ES outage must never fail a travel
 * create/update/delete. Failures are logged and swallowed.
 *
 * <p>The index endpoints require an authenticated caller; since these pushes
 * happen inside an authenticated manager/admin request, we forward that same
 * JWT cookie on the outbound call.
 */
@Component
public class SearchIndexClient {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexClient.class);
    private static final String BASE = "http://search-service/api/search/index";

    private final RestClient restClient;
    private final String cookieName;

    public SearchIndexClient(RestClient.Builder loadBalancedRestClientBuilder,
                             @Value("${app.cookie.name}") String cookieName) {
        this.restClient = loadBalancedRestClientBuilder.build();
        this.cookieName = cookieName;
    }

    public void index(Travel travel) {
        try {
            restClient.post()
                    .uri(BASE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(this::forwardAuthCookie)
                    .body(toPayload(travel))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to index travel {} in search-service: {}", travel.getId(), ex.getMessage());
        }
    }

    public void delete(Long travelId) {
        try {
            restClient.delete()
                    .uri(BASE + "/{id}", travelId)
                    .headers(this::forwardAuthCookie)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to remove travel {} from search index: {}", travelId, ex.getMessage());
        }
    }

    private Map<String, Object> toPayload(Travel travel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", travel.getId());
        body.put("title", travel.getTitle());
        body.put("destinationCities", travel.getDestinations().stream().map(d -> d.getCity()).toList());
        body.put("destinationCountries", travel.getDestinations().stream().map(d -> d.getCountry()).toList());
        body.put("activities", travel.getActivities().stream().map(a -> a.getName()).toList());
        body.put("accommodations", travel.getAccommodations().stream().map(a -> a.getName()).toList());
        body.put("transportationTypes", travel.getTransportations().stream().map(t -> t.getType()).toList());
        body.put("startDate", travel.getStartDate());
        body.put("endDate", travel.getEndDate());
        body.put("durationDays", (int) (ChronoUnit.DAYS.between(travel.getStartDate(), travel.getEndDate()) + 1));
        body.put("managerId", travel.getManagerId());
        return body;
    }

    private void forwardAuthCookie(HttpHeaders headers) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        if (request.getCookies() == null) {
            return;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                headers.add(HttpHeaders.COOKIE, cookieName + "=" + cookie.getValue());
                return;
            }
        }
    }
}
