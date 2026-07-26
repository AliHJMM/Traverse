package com.traverse.travel.repository;

import com.traverse.travel.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByTravelIdAndTravelerId(Long travelId, Long travelerId);

    List<Feedback> findByTravelId(Long travelId);

    List<Feedback> findByTravelerId(Long travelerId);

    List<Feedback> findByTravelIdIn(List<Long> travelIds);
}
