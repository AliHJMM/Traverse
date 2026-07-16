package com.traverse.payment.gateway;

import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.traverse.payment.entity.PaymentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentGatewayClient implements PaymentGatewayClient {

    private final RequestOptions requestOptions;

    public StripePaymentGatewayClient(@Value("${payment.stripe.secret-key}") String secretKey) {
        this.requestOptions = RequestOptions.builder().setApiKey(secretKey).build();
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.STRIPE;
    }

    /**
     * "token" here is a Stripe PaymentMethod id (pm_xxx) that the client
     * already created via Stripe.js/Elements -- this backend never sees a
     * raw card number, it just retrieves the safe display details Stripe
     * already tokenized.
     */
    @Override
    public AttachedPaymentMethod attach(String token) {
        try {
            com.stripe.model.PaymentMethod stripePaymentMethod =
                    com.stripe.model.PaymentMethod.retrieve(token, requestOptions);
            com.stripe.model.PaymentMethod.Card card = stripePaymentMethod.getCard();
            if (card == null) {
                throw new PaymentGatewayException("Stripe payment method " + token + " is not a card");
            }
            return new AttachedPaymentMethod(
                    stripePaymentMethod.getId(),
                    card.getBrand(),
                    card.getLast4(),
                    card.getExpMonth() == null ? null : card.getExpMonth().intValue(),
                    card.getExpYear() == null ? null : card.getExpYear().intValue(),
                    null);
        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe rejected payment method " + token, e);
        }
    }

    @Override
    public void detach(String externalId) {
        try {
            com.stripe.model.PaymentMethod stripePaymentMethod =
                    com.stripe.model.PaymentMethod.retrieve(externalId, requestOptions);
            stripePaymentMethod.detach(requestOptions);
        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe failed to detach payment method " + externalId, e);
        }
    }
}
