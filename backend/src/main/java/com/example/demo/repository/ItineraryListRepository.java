package com.example.demo.repository;

import com.example.demo.model.ItineraryList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryListRepository extends JpaRepository<ItineraryList, Long> {

    List<ItineraryList> findByTravelerId(Long travelerId);

    @Query("SELECT l FROM ItineraryList l JOIN l.sharedUsers u WHERE u.id = :userId")
    List<ItineraryList> findListsSharedWithUser(@Param("userId") Long userId);
}