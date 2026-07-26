package com.traverse.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A recorded payment for a travel booking. The provider charge itself is
 * executed through the {@link com.traverse.payment.gateway.PaymentGatewayClient}
 * against a previously-saved (tokenized) payment method; this row is the
 * durable ledger entry.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "travel_id", nullable = false)
    private Long travelId;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "external_charge_id")
    private String externalChargeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Payment() {
    }

    public Payment(Long userId, Long travelId, Long paymentMethodId, PaymentProvider provider, BigDecimal amount,
                   String currency, PaymentStatus status, String externalChargeId) {
        this.userId = userId;
        this.travelId = travelId;
        this.paymentMethodId = paymentMethodId;
        this.provider = provider;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.externalChargeId = externalChargeId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTravelId() {
        return travelId;
    }

    public Long getPaymentMethodId() {
        return paymentMethodId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getExternalChargeId() {
        return externalChargeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
