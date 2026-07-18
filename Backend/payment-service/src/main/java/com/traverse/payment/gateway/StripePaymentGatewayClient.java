package com.traverse.payment.gateway;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.traverse.payment.entity.PaymentProvider;
import com.traverse.payment.entity.StripeCustomerMapping;
import com.traverse.payment.repository.StripeCustomerMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StripePaymentGatewayClient implements PaymentGatewayClient {

    private final RequestOptions requestOptions;
    private final StripeCustomerMappingRepository stripeCustomerMappingRepository;

    public StripePaymentGatewayClient(@Value("${payment.stripe.secret-key}") String secretKey,
                                       StripeCustomerMappingRepository stripeCustomerMappingRepository) {
        this.requestOptions = RequestOptions.builder().setApiKey(secretKey).build();
        this.stripeCustomerMappingRepository = stripeCustomerMappingRepository;
    }

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.STRIPE;
    }

    /**
     * "token" is a Stripe PaymentMethod id (pm_xxx) that the client already
     * created via Stripe.js/Elements -- this backend never sees a raw card
     * number. Stripe requires a PaymentMethod to be attached to a Customer
     * before it can later be detached, so every user gets one Stripe
     * Customer (created on their first saved card, reused after that) and
     * each new payment method is attached to it.
     */
    @Override
    @Transactional
    public AttachedPaymentMethod attach(Long userId, String token) {
        try {
            String customerId = findOrCreateCustomer(userId);

            com.stripe.model.PaymentMethod stripePaymentMethod =
                    com.stripe.model.PaymentMethod.retrieve(token, requestOptions);
            stripePaymentMethod = stripePaymentMethod.attach(
                    PaymentMethodAttachParams.builder().setCustomer(customerId).build(), requestOptions);

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

    private String findOrCreateCustomer(Long userId) {
        return stripeCustomerMappingRepository.findById(userId)
                .map(StripeCustomerMapping::getCustomerId)
                .orElseGet(() -> createCustomer(userId));
    }

    private String createCustomer(Long userId) {
        try {
            Customer customer = Customer.create(
                    CustomerCreateParams.builder().putMetadata("userId", String.valueOf(userId)).build(),
                    requestOptions);
            stripeCustomerMappingRepository.save(new StripeCustomerMapping(userId, customer.getId()));
            return customer.getId();
        } catch (StripeException e) {
            throw new PaymentGatewayException("Stripe failed to create a customer for user " + userId, e);
        }
    }
}
