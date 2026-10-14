package com.example.demo.repository;

import com.example.demo.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByTravelerId(Long travelerId);
    List<Reservation> findByTripId(Long tripId);

    //Trova tutte le prenotazioni relative ai viaggi creati da una specifica agenzia ---
    List<Reservation> findByTripOrganizerEmail(String organizerEmail);
}
