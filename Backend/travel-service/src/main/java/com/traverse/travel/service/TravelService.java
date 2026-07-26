package com.traverse.travel.service;

import com.traverse.travel.dto.AccommodationRequest;
import com.traverse.travel.dto.ActivityRequest;
import com.traverse.travel.dto.CreateTravelRequest;
import com.traverse.travel.dto.DestinationRequest;
import com.traverse.travel.dto.TransportationRequest;
import com.traverse.travel.dto.UpdateTravelRequest;
import com.traverse.travel.entity.Accommodation;
import com.traverse.travel.entity.Activity;
import com.traverse.travel.entity.Destination;
import com.traverse.travel.entity.Transportation;
import com.traverse.travel.entity.Travel;
import com.traverse.travel.exception.TravelAccessDeniedException;
import com.traverse.travel.exception.TravelNotFoundException;
import com.traverse.travel.repository.TravelRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TravelService {

    private final TravelRepository travelRepository;
    private final DestinationGraphService destinationGraphService;

    public TravelService(TravelRepository travelRepository, DestinationGraphService destinationGraphService) {
        this.travelRepository = travelRepository;
        this.destinationGraphService = destinationGraphService;
    }

    public Travel create(CreateTravelRequest request, Long managerId) {
        Travel travel = new Travel(request.title(), request.startDate(), request.endDate());
        travel.setManagerId(managerId);
        populate(travel, request.destinations(), request.activities(), request.accommodations(), request.transportations());
        Travel saved = travelRepository.save(travel);

        destinationGraphService.syncItinerary(request.destinations());
        destinationGraphService.syncTravelFeatures(saved.getId(), request.destinations(), request.activities(),
                request.accommodations(), request.transportations());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Travel> findAll() {
        List<Travel> travels = travelRepository.findAll();
        travels.forEach(this::initializeCollections);
        return travels;
    }

    @Transactional(readOnly = true)
    public List<Travel> findByManager(Long managerId) {
        List<Travel> travels = travelRepository.findByManagerId(managerId);
        travels.forEach(this::initializeCollections);
        return travels;
    }

    @Transactional(readOnly = true)
    public Travel findById(Long id) {
        Travel travel = travelRepository.findById(id).orElseThrow(() -> new TravelNotFoundException(id));
        initializeCollections(travel);
        return travel;
    }

    public Travel update(Long id, UpdateTravelRequest request, Long callerId, boolean admin) {
        Travel travel = findById(id);
        assertCanManage(travel, callerId, admin);
        travel.setTitle(request.title());
        travel.setStartDate(request.startDate());
        travel.setEndDate(request.endDate());

        // Full replace of nested collections -- orphanRemoval on the Travel
        // entity cascades the deletes for whatever was there before.
        travel.getDestinations().clear();
        travel.getActivities().clear();
        travel.getAccommodations().clear();
        travel.getTransportations().clear();
        populate(travel, request.destinations(), request.activities(), request.accommodations(), request.transportations());

        Travel saved = travelRepository.save(travel);
        destinationGraphService.syncItinerary(request.destinations());
        destinationGraphService.syncTravelFeatures(id, request.destinations(), request.activities(),
                request.accommodations(), request.transportations());
        return saved;
    }

    public void delete(Long id, Long callerId, boolean admin) {
        Travel travel = travelRepository.findById(id).orElseThrow(() -> new TravelNotFoundException(id));
        assertCanManage(travel, callerId, admin);
        // destinations/activities/accommodations/transportations cascade via
        // orphanRemoval -- no separate delete calls needed.
        travelRepository.deleteById(id);
        destinationGraphService.deleteTravelNode(id);
    }

    /**
     * Neo4j-backed personalized recommendations: ranked travel ids come from
     * the graph, then the full travels are loaded from Postgres (the source
     * of truth) preserving the recommendation order.
     */
    @Transactional(readOnly = true)
    public List<Travel> recommendFor(Long userId) {
        List<Long> rankedIds = destinationGraphService.recommendTravelIds(userId);
        if (rankedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Travel> byId = new LinkedHashMap<>();
        travelRepository.findAllById(rankedIds).forEach(t -> byId.put(t.getId(), t));
        return rankedIds.stream()
                .map(byId::get)
                .filter(t -> t != null)
                .map(t -> {
                    initializeCollections(t);
                    return t;
                })
                .toList();
    }

    private void assertCanManage(Travel travel, Long callerId, boolean admin) {
        if (admin) {
            return;
        }
        if (travel.getManagerId() == null || !travel.getManagerId().equals(callerId)) {
            throw new TravelAccessDeniedException(travel.getId());
        }
    }

    /**
     * The controller maps entities to response DTOs after this method
     * returns, once the transaction (and Hibernate session) has already
     * closed -- open-in-view is deliberately off, so the four @OneToMany
     * collections (lazy by default) would otherwise throw
     * LazyInitializationException the first time the controller touches
     * them. Initializing them here, still inside the transaction, is the
     * fix. (A single @EntityGraph across all four isn't an option: Hibernate
     * throws MultipleBagFetchException when more than one List-typed
     * collection is eagerly joined in the same query.)
     */
    private void initializeCollections(Travel travel) {
        Hibernate.initialize(travel.getDestinations());
        Hibernate.initialize(travel.getActivities());
        Hibernate.initialize(travel.getAccommodations());
        Hibernate.initialize(travel.getTransportations());
    }

    private void populate(Travel travel, List<DestinationRequest> destinations, List<ActivityRequest> activities,
                           List<AccommodationRequest> accommodations, List<TransportationRequest> transportations) {
        for (DestinationRequest d : destinations) {
            travel.addDestination(new Destination(d.city(), d.country(), d.arrivalDate(), d.departureDate()));
        }
        if (activities != null) {
            for (ActivityRequest a : activities) {
                travel.addActivity(new Activity(a.name(), a.description(), a.destinationCity(), a.date(), a.cost()));
            }
        }
        if (accommodations != null) {
            for (AccommodationRequest a : accommodations) {
                travel.addAccommodation(new Accommodation(a.name(), a.type(), a.address(), a.checkIn(), a.checkOut()));
            }
        }
        if (transportations != null) {
            for (TransportationRequest t : transportations) {
                travel.addTransportation(new Transportation(t.type(), t.provider(), t.fromLocation(), t.toLocation(),
                        t.departureTime(), t.arrivalTime()));
            }
        }
    }
}
