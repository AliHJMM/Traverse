package com.traverse.payment.repository;

import com.traverse.payment.entity.StripeCustomerMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeCustomerMappingRepository extends JpaRepository<StripeCustomerMapping, Long> {
}
