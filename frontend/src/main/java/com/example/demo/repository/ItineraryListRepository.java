package com.example.demo.repository;

import com.example.demo.model.ItineraryList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryListRepository extends JpaRepository<ItineraryList, Long> {

    List<ItineraryList> findByTravelerId(Long travelerId);
}