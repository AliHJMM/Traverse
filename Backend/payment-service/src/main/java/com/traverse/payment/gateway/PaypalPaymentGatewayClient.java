package com.traverse.payment.gateway;

import com.traverse.payment.entity.PaymentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * PayPal's REST API is simple enough to call directly rather than pulling in
 * their full SDK. "token" is a PayPal payment-source token the client
 * already created via the PayPal JS SDK -- this just resolves it against
 * PayPal's v3 Vault "Payment Tokens" API for safe display details.
 */
@Component
public class PaypalPaymentGatewayClient implements PaymentGatewayClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public PaypalPaymentGatewayClient(@Value("${payment.paypal.base-uri}") String baseUri,
                                       @Value("${payment.paypal.client-id}") String clientId,
                                       @Value("${payment.paypal.client-secret}") String clientSecret) {
        this.restClient = RestClient.create(baseUri);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.PAYPAL;
    }

    /**
     * Unlike Stripe, a PayPal v3 Vault payment token is already tied to a
     * specific payer from the vaulting flow that created it -- there's no
     * separate "customer" object to create or attach here, so userId isn't
     * needed on this path (kept only for interface parity with Stripe).
     */
    @Override
    public AttachedPaymentMethod attach(Long userId, String token) {
        String accessToken = fetchAccessToken();
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/v3/vault/payment-tokens/{id}", token)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return parsePaymentToken(response);
        } catch (RestClientException e) {
            throw new PaymentGatewayException("PayPal rejected payment token " + token, e);
        }
    }

    @Override
    public void detach(String externalId) {
        String accessToken = fetchAccessToken();
        try {
            restClient.delete()
                    .uri("/v3/vault/payment-tokens/{id}", externalId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new PaymentGatewayException("PayPal failed to detach payment token " + externalId, e);
        }
    }

    private String fetchAccessToken() {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/v1/oauth2/token")
                    .headers(h -> h.setBasicAuth(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            if (response == null || response.get("access_token") == null) {
                throw new PaymentGatewayException("PayPal did not return an access token");
            }
            return (String) response.get("access_token");
        } catch (RestClientException e) {
            throw new PaymentGatewayException("PayPal authentication failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private AttachedPaymentMethod parsePaymentToken(Map<String, Object> response) {
        if (response == null) {
            throw new PaymentGatewayException("PayPal returned an empty payment token response");
        }
        String id = (String) response.get("id");
        Map<String, Object> source = (Map<String, Object>) response.get("payment_source");

        if (source != null && source.get("card") != null) {
            Map<String, Object> card = (Map<String, Object>) source.get("card");
            String expiry = (String) card.get("expiry"); // "YYYY-MM"
            Integer expMonth = null;
            Integer expYear = null;
            if (expiry != null && expiry.contains("-")) {
                String[] parts = expiry.split("-");
                expYear = Integer.parseInt(parts[0]);
                expMonth = Integer.parseInt(parts[1]);
            }
            return new AttachedPaymentMethod(id, (String) card.get("brand"), (String) card.get("last_digits"),
                    expMonth, expYear, null);
        }

        if (source != null && source.get("paypal") != null) {
            Map<String, Object> paypal = (Map<String, Object>) source.get("paypal");
            return new AttachedPaymentMethod(id, null, null, null, null, (String) paypal.get("email_address"));
        }

        throw new PaymentGatewayException("Unrecognized PayPal payment source in token " + id);
    }
}
