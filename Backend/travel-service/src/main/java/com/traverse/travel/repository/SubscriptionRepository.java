package com.traverse.travel.repository;

import com.traverse.travel.entity.Subscription;
import com.traverse.travel.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTravelIdAndTravelerIdAndStatus(Long travelId, Long travelerId, SubscriptionStatus status);

    List<Subscription> findByTravelIdAndStatus(Long travelId, SubscriptionStatus status);

    List<Subscription> findByTravelerIdAndStatus(Long travelerId, SubscriptionStatus status);

    long countByTravelerIdAndStatus(Long travelerId, SubscriptionStatus status);

    long countByTravelIdAndStatus(Long travelId, SubscriptionStatus status);
}
