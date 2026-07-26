package com.traverse.travel.service;

import com.traverse.travel.dto.AccommodationRequest;
import com.traverse.travel.dto.ActivityRequest;
import com.traverse.travel.dto.DestinationRequest;
import com.traverse.travel.dto.NearbyDestinationResponse;
import com.traverse.travel.dto.TransportationRequest;
import com.traverse.travel.graph.DestinationNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA (Postgres) and Neo4j each register their own PlatformTransactionManager
 * in this app; Spring can't pick one automatically, so every method here
 * must explicitly pin "neo4jTransactionManager" -- otherwise repository
 * calls fail with a NullPointerException from Spring Data's internal
 * TransactionTemplate.
 */
@Service
public class DestinationGraphService {

    private final DestinationNodeRepository destinationNodeRepository;

    public DestinationGraphService(DestinationNodeRepository destinationNodeRepository) {
        this.destinationNodeRepository = destinationNodeRepository;
    }

    /**
     * Connects each consecutive pair of destinations in the order they
     * appear on the itinerary (A -> B -> C becomes A-B and B-C edges),
     * bumping the trip count if the connection already exists.
     */
    @Transactional("neo4jTransactionManager")
    public void syncItinerary(List<DestinationRequest> destinations) {
        for (int i = 0; i < destinations.size() - 1; i++) {
            DestinationRequest from = destinations.get(i);
            DestinationRequest to = destinations.get(i + 1);
            destinationNodeRepository.connect(from.city(), from.country(), to.city(), to.country());
        }
    }

    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public List<NearbyDestinationResponse> findNearby(String city) {
        return destinationNodeRepository.findNearby(city).stream()
                .map(node -> new NearbyDestinationResponse(node.getCity(), node.getCountry()))
                .toList();
    }

    // --- Part 2: travel feature graph + recommendations ------------------

    /**
     * Mirrors a travel's feature set into the graph (destinations,
     * activities, accommodation types, transport types) so recommendations
     * can be computed by feature overlap. Called on travel create/update;
     * clears the travel's old feature edges first so an update is a clean
     * replace.
     */
    @Transactional("neo4jTransactionManager")
    public void syncTravelFeatures(Long travelId, List<DestinationRequest> destinations,
                                   List<ActivityRequest> activities, List<AccommodationRequest> accommodations,
                                   List<TransportationRequest> transportations) {
        destinationNodeRepository.resetTravelFeatures(travelId);
        if (destinations != null) {
            for (DestinationRequest d : destinations) {
                destinationNodeRepository.linkDestination(travelId, d.city(), d.country());
            }
        }
        if (activities != null) {
            for (ActivityRequest a : activities) {
                if (a.name() != null && !a.name().isBlank()) {
                    destinationNodeRepository.linkActivity(travelId, a.name());
                }
            }
        }
        if (accommodations != null) {
            for (AccommodationRequest a : accommodations) {
                if (a.type() != null && !a.type().isBlank()) {
                    destinationNodeRepository.linkAccommodation(travelId, a.type());
                }
            }
        }
        if (transportations != null) {
            for (TransportationRequest t : transportations) {
                if (t.type() != null && !t.type().isBlank()) {
                    destinationNodeRepository.linkTransport(travelId, t.type());
                }
            }
        }
    }

    @Transactional("neo4jTransactionManager")
    public void deleteTravelNode(Long travelId) {
        destinationNodeRepository.deleteTravelNode(travelId);
    }

    @Transactional("neo4jTransactionManager")
    public void recordParticipation(Long userId, Long travelId) {
        destinationNodeRepository.recordParticipation(userId, travelId);
    }

    @Transactional("neo4jTransactionManager")
    public void removeParticipation(Long userId, Long travelId) {
        destinationNodeRepository.removeParticipation(userId, travelId);
    }

    @Transactional("neo4jTransactionManager")
    public void recordRating(Long userId, Long travelId, int score) {
        destinationNodeRepository.recordRating(userId, travelId, score);
    }

    @Transactional(value = "neo4jTransactionManager", readOnly = true)
    public List<Long> recommendTravelIds(Long userId) {
        return destinationNodeRepository.recommendTravelIds(userId);
    }
}
