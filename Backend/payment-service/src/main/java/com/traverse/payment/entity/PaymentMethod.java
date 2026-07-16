package com.traverse.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * We never store raw card numbers -- only the opaque token/id the provider
 * (Stripe/PayPal) hands back after tokenizing a card or linking a PayPal
 * account, plus safe display metadata (brand/last4/expiry or payer email).
 */
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    private String brand;

    private String last4;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Column(name = "payer_email")
    private String payerEmail;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PaymentMethod() {
    }

    public PaymentMethod(Long userId, PaymentProvider provider, String externalId, String brand, String last4,
                          Integer expiryMonth, Integer expiryYear, String payerEmail, boolean isDefault) {
        this.userId = userId;
        this.provider = provider;
        this.externalId = externalId;
        this.brand = brand;
        this.last4 = last4;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.payerEmail = payerEmail;
        this.isDefault = isDefault;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getBrand() {
        return brand;
    }

    public String getLast4() {
        return last4;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
