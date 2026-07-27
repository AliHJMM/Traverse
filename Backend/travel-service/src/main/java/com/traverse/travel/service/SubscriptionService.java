package com.traverse.travel.service;

import com.traverse.travel.dto.SubscriberResponse;
import com.traverse.travel.dto.SubscriptionResponse;
import com.traverse.travel.entity.Subscription;
import com.traverse.travel.entity.SubscriptionStatus;
import com.traverse.travel.entity.Travel;
import com.traverse.travel.exception.SubscriptionCutoffException;
import com.traverse.travel.exception.SubscriptionStateException;
import com.traverse.travel.exception.TravelAccessDeniedException;
import com.traverse.travel.exception.TravelNotFoundException;
import com.traverse.travel.repository.SubscriptionRepository;
import com.traverse.travel.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SubscriptionService {

    /** No subscribe/unsubscribe within this many days of departure. */
    private static final int CUTOFF_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;
    private final TravelRepository travelRepository;
    private final DestinationGraphService destinationGraphService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, TravelRepository travelRepository,
                               DestinationGraphService destinationGraphService) {
        this.subscriptionRepository = subscriptionRepository;
        this.travelRepository = travelRepository;
        this.destinationGraphService = destinationGraphService;
    }

    public SubscriptionResponse subscribe(Long travelId, Long travelerId) {
        Travel travel = travelRepository.findById(travelId).orElseThrow(() -> new TravelNotFoundException(travelId));
        assertBeforeCutoff(travel);
        subscriptionRepository.findByTravelIdAndTravelerIdAndStatus(travelId, travelerId, SubscriptionStatus.SUBSCRIBED)
                .ifPresent(s -> {
                    throw new SubscriptionStateException("Already subscribed to this travel");
                });

        Subscription saved = subscriptionRepository.save(new Subscription(travelId, travelerId));
        destinationGraphService.recordParticipation(travelerId, travelId);
        return toResponse(saved);
    }

    public void unsubscribe(Long travelId, Long travelerId) {
        Travel travel = travelRepository.findById(travelId).orElseThrow(() -> new TravelNotFoundException(travelId));
        Subscription subscription = subscriptionRepository
                .findByTravelIdAndTravelerIdAndStatus(travelId, travelerId, SubscriptionStatus.SUBSCRIBED)
                .orElseThrow(() -> new SubscriptionStateException("Not subscribed to this travel"));
        assertBeforeCutoff(travel);

        subscription.cancel();
        subscriptionRepository.save(subscription);
        destinationGraphService.removeParticipation(travelerId, travelId);
    }

    @Transactional(readOnly = true)
    public List<SubscriberResponse> subscribersOf(Long travelId, Long callerId, boolean admin) {
        Travel travel = travelRepository.findById(travelId).orElseThrow(() -> new TravelNotFoundException(travelId));
        assertCanManage(travel, callerId, admin);
        return subscriptionRepository.findByTravelIdAndStatus(travelId, SubscriptionStatus.SUBSCRIBED).stream()
                .map(s -> new SubscriberResponse(s.getTravelerId(), s.getCreatedAt()))
                .toList();
    }

    /** A manager (owner) or admin removes a subscriber from their travel. */
    public void removeSubscriber(Long travelId, Long travelerId, Long callerId, boolean admin) {
        Travel travel = travelRepository.findById(travelId).orElseThrow(() -> new TravelNotFoundException(travelId));
        assertCanManage(travel, callerId, admin);
        Subscription subscription = subscriptionRepository
                .findByTravelIdAndTravelerIdAndStatus(travelId, travelerId, SubscriptionStatus.SUBSCRIBED)
                .orElseThrow(() -> new SubscriptionStateException("That traveler is not subscribed"));
        subscription.cancel();
        subscriptionRepository.save(subscription);
        destinationGraphService.removeParticipation(travelerId, travelId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> mySubscriptions(Long travelerId) {
        return subscriptionRepository.findByTravelerIdAndStatus(travelerId, SubscriptionStatus.SUBSCRIBED).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Full subscription history (all statuses) for the traveler's profile. */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> subscriptionHistory(Long travelerId) {
        return subscriptionRepository.findByTravelerIdOrderByCreatedAtDesc(travelerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void assertBeforeCutoff(Travel travel) {
        if (LocalDate.now().isAfter(travel.getStartDate().minusDays(CUTOFF_DAYS))) {
            throw new SubscriptionCutoffException();
        }
    }

    private void assertCanManage(Travel travel, Long callerId, boolean admin) {
        if (admin) {
            return;
        }
        if (travel.getManagerId() == null || !travel.getManagerId().equals(callerId)) {
            throw new TravelAccessDeniedException(travel.getId());
        }
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return new SubscriptionResponse(s.getId(), s.getTravelId(), s.getTravelerId(), s.getStatus(), s.getCreatedAt());
    }
}
