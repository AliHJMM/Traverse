package com.traverse.payment.service;

import com.traverse.payment.dto.ChargeRequest;
import com.traverse.payment.entity.Payment;
import com.traverse.payment.entity.PaymentMethod;
import com.traverse.payment.entity.PaymentProvider;
import com.traverse.payment.entity.PaymentStatus;
import com.traverse.payment.exception.PaymentMethodNotFoundException;
import com.traverse.payment.gateway.PaymentGatewayClient;
import com.traverse.payment.gateway.PaymentGatewayException;
import com.traverse.payment.repository.PaymentMethodRepository;
import com.traverse.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Executes travel-booking charges against a traveler's own saved payment
 * method and records them in the ledger. Both the successful and failed
 * outcomes are persisted so dashboards and history reflect reality.
 */
@Service
@Transactional
public class PaymentService {

    private static final String CURRENCY = "USD";

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TravelPricingClient travelPricingClient;
    private final List<PaymentGatewayClient> gatewayClients;

    public PaymentService(PaymentRepository paymentRepository, PaymentMethodRepository paymentMethodRepository,
                          TravelPricingClient travelPricingClient, List<PaymentGatewayClient> gatewayClients) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.travelPricingClient = travelPricingClient;
        this.gatewayClients = gatewayClients;
    }

    public Payment charge(Long userId, ChargeRequest request) {
        PaymentMethod method = paymentMethodRepository.findById(request.paymentMethodId())
                .orElseThrow(() -> new PaymentMethodNotFoundException(request.paymentMethodId()));
        // A traveler may only pay with a payment method they own.
        if (!method.getUserId().equals(userId)) {
            throw new PaymentMethodNotFoundException(request.paymentMethodId());
        }

        // Authoritative amount comes from travel-service, never the client.
        BigDecimal amount = travelPricingClient.priceOf(request.travelId());
        long amountMinor = amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        PaymentProvider provider = method.getProvider();

        // A declined charge is recorded (FAILED) rather than thrown, so the
        // ledger always reflects the attempt and the same DB transaction isn't
        // marked rollback-only. The controller translates a FAILED result into
        // a 402 Payment Required.
        try {
            String chargeId = gatewayFor(provider).charge(userId, method.getExternalId(), amountMinor, CURRENCY);
            return paymentRepository.save(new Payment(userId, request.travelId(), method.getId(), provider,
                    amount, CURRENCY, PaymentStatus.SUCCEEDED, chargeId));
        } catch (PaymentGatewayException ex) {
            return paymentRepository.save(new Payment(userId, request.travelId(), method.getId(), provider,
                    amount, CURRENCY, PaymentStatus.FAILED, null));
        }
    }

    @Transactional(readOnly = true)
    public List<Payment> history(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private PaymentGatewayClient gatewayFor(PaymentProvider provider) {
        return gatewayClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new PaymentGatewayException("No gateway client configured for provider " + provider));
    }
}
