package com.traverse.travel.controller;

import com.traverse.travel.dto.SubscriberResponse;
import com.traverse.travel.dto.SubscriptionResponse;
import com.traverse.travel.entity.Role;
import com.traverse.travel.security.AuthenticatedUser;
import com.traverse.travel.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/travels")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** Traveler subscribes to a travel (any authenticated role may). */
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<SubscriptionResponse> subscribe(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(id, principal.id()));
    }

    /** Traveler unsubscribes (subject to the 3-day cutoff). */
    @DeleteMapping("/{id}/subscribe")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        subscriptionService.unsubscribe(id, principal.id());
        return ResponseEntity.noContent().build();
    }

    /** Manager (owner) / admin views the subscriber list for a travel. */
    @GetMapping("/{id}/subscribers")
    public List<SubscriberResponse> subscribers(@PathVariable Long id,
                                                @AuthenticationPrincipal AuthenticatedUser principal) {
        return subscriptionService.subscribersOf(id, principal.id(), isAdmin(principal));
    }

    /** Manager (owner) / admin removes a subscriber from their travel. */
    @DeleteMapping("/{id}/subscribers/{travelerId}")
    public ResponseEntity<Void> removeSubscriber(@PathVariable Long id, @PathVariable Long travelerId,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        subscriptionService.removeSubscriber(id, travelerId, principal.id(), isAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    /** The current traveler's own active subscriptions (their trips). */
    @GetMapping("/subscriptions/mine")
    public List<SubscriptionResponse> mySubscriptions(@AuthenticationPrincipal AuthenticatedUser principal) {
        return subscriptionService.mySubscriptions(principal.id());
    }

    /** The current traveler's full participation history (all statuses). */
    @GetMapping("/subscriptions/history")
    public List<SubscriptionResponse> myHistory(@AuthenticationPrincipal AuthenticatedUser principal) {
        return subscriptionService.subscriptionHistory(principal.id());
    }

    private boolean isAdmin(AuthenticatedUser principal) {
        return principal != null && principal.role() == Role.ADMIN;
    }
}
