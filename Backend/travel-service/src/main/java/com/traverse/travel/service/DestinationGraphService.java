package com.traverse.travel.service;

import com.traverse.travel.dto.DestinationRequest;
import com.traverse.travel.dto.NearbyDestinationResponse;
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
}
