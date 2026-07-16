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
import com.traverse.travel.exception.TravelNotFoundException;
import com.traverse.travel.repository.TravelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TravelService {

    private final TravelRepository travelRepository;
    private final DestinationGraphService destinationGraphService;

    public TravelService(TravelRepository travelRepository, DestinationGraphService destinationGraphService) {
        this.travelRepository = travelRepository;
        this.destinationGraphService = destinationGraphService;
    }

    public Travel create(CreateTravelRequest request) {
        Travel travel = new Travel(request.title(), request.startDate(), request.endDate());
        populate(travel, request.destinations(), request.activities(), request.accommodations(), request.transportations());
        Travel saved = travelRepository.save(travel);

        destinationGraphService.syncItinerary(request.destinations());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Travel> findAll() {
        return travelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Travel findById(Long id) {
        return travelRepository.findById(id).orElseThrow(() -> new TravelNotFoundException(id));
    }

    public Travel update(Long id, UpdateTravelRequest request) {
        Travel travel = findById(id);
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
        return saved;
    }

    public void delete(Long id) {
        if (!travelRepository.existsById(id)) {
            throw new TravelNotFoundException(id);
        }
        // destinations/activities/accommodations/transportations cascade via
        // orphanRemoval -- no separate delete calls needed.
        travelRepository.deleteById(id);
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
