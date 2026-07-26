package com.traverse.travel.controller;

import com.traverse.travel.dto.CreateFeedbackRequest;
import com.traverse.travel.dto.FeedbackResponse;
import com.traverse.travel.security.AuthenticatedUser;
import com.traverse.travel.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/travels")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** Traveler submits/updates feedback for a travel they participated in. */
    @PostMapping("/{id}/feedback")
    public ResponseEntity<FeedbackResponse> submit(@PathVariable Long id,
                                                   @Valid @RequestBody CreateFeedbackRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.submit(id, principal.id(), request));
    }

    /** Feedback for a travel -- visible to managers, admins, and travelers. */
    @GetMapping("/{id}/feedback")
    public List<FeedbackResponse> forTravel(@PathVariable Long id) {
        return feedbackService.forTravel(id);
    }

    /** The current traveler's own feedback history. */
    @GetMapping("/feedback/mine")
    public List<FeedbackResponse> mine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return feedbackService.mine(principal.id());
    }
}
