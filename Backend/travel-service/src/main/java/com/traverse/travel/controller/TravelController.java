package com.traverse.travel.controller;

import com.traverse.travel.dto.AccommodationResponse;
import com.traverse.travel.dto.ActivityResponse;
import com.traverse.travel.dto.CreateTravelRequest;
import com.traverse.travel.dto.DestinationResponse;
import com.traverse.travel.dto.NearbyDestinationResponse;
import com.traverse.travel.dto.TransportationResponse;
import com.traverse.travel.dto.TravelResponse;
import com.traverse.travel.dto.UpdateTravelRequest;
import com.traverse.travel.entity.Role;
import com.traverse.travel.entity.Travel;
import com.traverse.travel.security.AuthenticatedUser;
import com.traverse.travel.service.DestinationGraphService;
import com.traverse.travel.service.TravelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/travels")
public class TravelController {

    private final TravelService travelService;
    private final DestinationGraphService destinationGraphService;

    public TravelController(TravelService travelService, DestinationGraphService destinationGraphService) {
        this.travelService = travelService;
        this.destinationGraphService = destinationGraphService;
    }

    @PostMapping
    public ResponseEntity<TravelResponse> create(@Valid @RequestBody CreateTravelRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        Travel travel = travelService.create(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(travel));
    }

    /** Browse all travels (any authenticated role -- travelers included). */
    @GetMapping
    public List<TravelResponse> findAll() {
        return travelService.findAll().stream().map(this::toResponse).toList();
    }

    /** A Travel Manager's own listings. */
    @GetMapping("/mine")
    public List<TravelResponse> findMine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return travelService.findByManager(principal.id()).stream().map(this::toResponse).toList();
    }

    /** Neo4j personalized recommendations for the current traveler. */
    @GetMapping("/recommendations")
    public List<TravelResponse> recommendations(@AuthenticationPrincipal AuthenticatedUser principal) {
        return travelService.recommendFor(principal.id()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TravelResponse findById(@PathVariable Long id) {
        return toResponse(travelService.findById(id));
    }

    @PutMapping("/{id}")
    public TravelResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTravelRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return toResponse(travelService.update(id, request, principal.id(), isAdmin(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        travelService.delete(id, principal.id(), isAdmin(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/destinations/{city}/nearby")
    public List<NearbyDestinationResponse> nearby(@PathVariable String city) {
        return destinationGraphService.findNearby(city);
    }

    private boolean isAdmin(AuthenticatedUser principal) {
        return principal != null && principal.role() == Role.ADMIN;
    }

    private TravelResponse toResponse(Travel travel) {
        List<DestinationResponse> destinations = travel.getDestinations().stream()
                .map(d -> new DestinationResponse(d.getId(), d.getCity(), d.getCountry(), d.getArrivalDate(), d.getDepartureDate()))
                .toList();
        List<ActivityResponse> activities = travel.getActivities().stream()
                .map(a -> new ActivityResponse(a.getId(), a.getName(), a.getDescription(), a.getDestinationCity(), a.getDate(), a.getCost()))
                .toList();
        List<AccommodationResponse> accommodations = travel.getAccommodations().stream()
                .map(a -> new AccommodationResponse(a.getId(), a.getName(), a.getType(), a.getAddress(), a.getCheckIn(), a.getCheckOut()))
                .toList();
        List<TransportationResponse> transportations = travel.getTransportations().stream()
                .map(t -> new TransportationResponse(t.getId(), t.getType(), t.getProvider(), t.getFromLocation(),
                        t.getToLocation(), t.getDepartureTime(), t.getArrivalTime()))
                .toList();

        return new TravelResponse(travel.getId(), travel.getTitle(), travel.getStartDate(), travel.getEndDate(),
                TravelResponse.computeDurationDays(travel.getStartDate(), travel.getEndDate()),
                destinations, activities, accommodations, transportations, travel.getCreatedAt());
    }
}
