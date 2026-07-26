package com.traverse.travel.service;

import com.traverse.travel.dto.CreateFeedbackRequest;
import com.traverse.travel.dto.FeedbackResponse;
import com.traverse.travel.entity.Feedback;
import com.traverse.travel.entity.SubscriptionStatus;
import com.traverse.travel.exception.SubscriptionStateException;
import com.traverse.travel.exception.TravelNotFoundException;
import com.traverse.travel.repository.FeedbackRepository;
import com.traverse.travel.repository.SubscriptionRepository;
import com.traverse.travel.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TravelRepository travelRepository;
    private final DestinationGraphService destinationGraphService;

    public FeedbackService(FeedbackRepository feedbackRepository, SubscriptionRepository subscriptionRepository,
                           TravelRepository travelRepository, DestinationGraphService destinationGraphService) {
        this.feedbackRepository = feedbackRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.travelRepository = travelRepository;
        this.destinationGraphService = destinationGraphService;
    }

    /** A traveler leaves (or updates) feedback for a travel they subscribed to. */
    public FeedbackResponse submit(Long travelId, Long travelerId, CreateFeedbackRequest request) {
        if (!travelRepository.existsById(travelId)) {
            throw new TravelNotFoundException(travelId);
        }
        subscriptionRepository.findByTravelIdAndTravelerIdAndStatus(travelId, travelerId, SubscriptionStatus.SUBSCRIBED)
                .orElseThrow(() -> new SubscriptionStateException("You can only review a travel you are subscribed to"));

        Feedback feedback = feedbackRepository.findByTravelIdAndTravelerId(travelId, travelerId)
                .map(existing -> {
                    existing.setRating(request.rating());
                    existing.setComment(request.comment());
                    return existing;
                })
                .orElseGet(() -> new Feedback(travelId, travelerId, request.rating(), request.comment()));

        Feedback saved = feedbackRepository.save(feedback);
        // Feed the rating into the graph so it weights recommendations.
        destinationGraphService.recordRating(travelerId, travelId, request.rating());
        return toResponse(saved);
    }

    /** All feedback for a travel (reviews are visible to any authenticated user). */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> forTravel(Long travelId) {
        return feedbackRepository.findByTravelId(travelId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> mine(Long travelerId) {
        return feedbackRepository.findByTravelerId(travelerId).stream().map(this::toResponse).toList();
    }

    private FeedbackResponse toResponse(Feedback f) {
        return new FeedbackResponse(f.getId(), f.getTravelId(), f.getTravelerId(), f.getRating(),
                f.getComment(), f.getCreatedAt());
    }
}
