package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/traveler/{travelerId}/trip/{tripId}")
    public ResponseEntity<ReservationDTO> createReservation(
            @PathVariable Long travelerId,
            @PathVariable Long tripId) {

        ReservationDTO reservation = reservationService.createReservation(travelerId, tripId);
        return new ResponseEntity<>(reservation, HttpStatus.CREATED);
    }

    @GetMapping("/traveler/{travelerId}")
    public ResponseEntity<List<ReservationDTO>> getTravelerReservations(@PathVariable Long travelerId) {
        return ResponseEntity.ok(reservationService.getReservationsByTraveler(travelerId));
    }
}