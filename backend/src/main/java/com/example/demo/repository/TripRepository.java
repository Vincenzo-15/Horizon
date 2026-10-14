package com.example.demo.repository;

import com.example.demo.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByOrganizerId(Long organizerId);

    List<Trip> findByAvailableSpotsGreaterThan(Integer spots);
}
