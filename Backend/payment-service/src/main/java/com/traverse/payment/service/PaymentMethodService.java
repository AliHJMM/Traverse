package com.traverse.payment.service;

import com.traverse.payment.dto.CreatePaymentMethodRequest;
import com.traverse.payment.entity.PaymentMethod;
import com.traverse.payment.entity.PaymentProvider;
import com.traverse.payment.exception.PaymentMethodNotFoundException;
import com.traverse.payment.gateway.AttachedPaymentMethod;
import com.traverse.payment.gateway.PaymentGatewayClient;
import com.traverse.payment.gateway.PaymentGatewayException;
import com.traverse.payment.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final List<PaymentGatewayClient> gatewayClients;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository, List<PaymentGatewayClient> gatewayClients) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.gatewayClients = gatewayClients;
    }

    public PaymentMethod create(CreatePaymentMethodRequest request) {
        AttachedPaymentMethod attached = gatewayFor(request.provider()).attach(request.userId(), request.token());

        if (request.setDefault()) {
            clearExistingDefault(request.userId());
        }

        PaymentMethod paymentMethod = new PaymentMethod(
                request.userId(), request.provider(), attached.externalId(), attached.brand(), attached.last4(),
                attached.expiryMonth(), attached.expiryYear(), attached.payerEmail(), request.setDefault());
        return paymentMethodRepository.save(paymentMethod);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> findAll() {
        return paymentMethodRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> findByUserId(Long userId) {
        return paymentMethodRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PaymentMethod findById(Long id) {
        return paymentMethodRepository.findById(id).orElseThrow(() -> new PaymentMethodNotFoundException(id));
    }

    public void delete(Long id) {
        PaymentMethod paymentMethod = findById(id);
        gatewayFor(paymentMethod.getProvider()).detach(paymentMethod.getExternalId());
        paymentMethodRepository.delete(paymentMethod);
    }

    /**
     * Only one default payment method per user -- setting a new default
     * cascades an unset onto whichever one previously held it.
     */
    private void clearExistingDefault(Long userId) {
        paymentMethodRepository.findByUserId(userId).stream()
                .filter(PaymentMethod::isDefault)
                .forEach(existing -> {
                    existing.setDefault(false);
                    paymentMethodRepository.save(existing);
                });
    }

    private PaymentGatewayClient gatewayFor(PaymentProvider provider) {
        return gatewayClients.stream()
                .filter(client -> client.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new PaymentGatewayException("No gateway client configured for provider " + provider));
    }
}
