package com.traverse.payment.service;

import com.traverse.payment.gateway.PaymentGatewayException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fetches the authoritative price of a travel from travel-service (the source
 * of truth) so a booking charge is never based on a client-supplied amount.
 * The caller's own auth cookie is forwarded, since the charge happens inside
 * that authenticated request.
 */
@Component
public class TravelPricingClient {

    private static final String BASE = "http://travel-service/api/travels";

    private final RestClient restClient;
    private final String cookieName;

    public TravelPricingClient(RestClient.Builder loadBalancedRestClientBuilder,
                               @Value("${app.cookie.name}") String cookieName) {
        this.restClient = loadBalancedRestClientBuilder.build();
        this.cookieName = cookieName;
    }

    /** The current price of the travel, or a gateway error if it can't be resolved. */
    public BigDecimal priceOf(Long travelId) {
        try {
            Map<String, Object> body = restClient.get()
                    .uri(BASE + "/{id}", travelId)
                    .headers(this::forwardAuthCookie)
                    .retrieve()
                    .body(Map.class);
            Object price = body == null ? null : body.get("price");
            if (price == null) {
                throw new PaymentGatewayException("Travel " + travelId + " has no price");
            }
            return new BigDecimal(price.toString());
        } catch (PaymentGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PaymentGatewayException("Could not resolve price for travel " + travelId, ex);
        }
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
