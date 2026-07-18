package com.traverse.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Stripe's model requires a PaymentMethod to be attached to a Customer
 * before it can later be detached (or reused) -- this maps our internal
 * userId to the one Stripe Customer we create for them, reused across all
 * of that user's saved cards instead of creating a new Customer every time.
 */
@Entity
@Table(name = "stripe_customers")
public class StripeCustomerMapping {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    protected StripeCustomerMapping() {
    }

    public StripeCustomerMapping(Long userId, String customerId) {
        this.userId = userId;
        this.customerId = customerId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
