package com.traverse.travel.repository;

import com.traverse.travel.entity.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelRepository extends JpaRepository<Travel, Long> {

    List<Travel> findByManagerId(Long managerId);
}
