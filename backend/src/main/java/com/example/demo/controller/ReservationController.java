package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // L'app Android invierà una chiamata POST a /api/reservations/create/1?spots=2
    // Usiamo Principal per prendere l'email direttamente dal Token in totale sicurezza
    @PostMapping("/create/{tripId}")
    public ResponseEntity<?> createReservation(
            @PathVariable Long tripId,
            @RequestParam(defaultValue = "1") Integer spots,
            Principal principal) {

        try {
            // Passiamo i 3 parametri richiesti dal servizio: ID viaggio, numero posti, email utente
            ReservationDTO reservation = reservationService.createReservation(tripId, spots, principal.getName());
            return new ResponseEntity<>(reservation, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Storico prenotazioni per il viaggiatore loggato
    @GetMapping("/my-reservations")
    public ResponseEntity<List<ReservationDTO>> getMyReservations(Principal principal) {
        return ResponseEntity.ok(reservationService.getReservationsByTraveler(principal.getName()));
    }

    // Ordini ricevuti per l'agenzia loggata
    @GetMapping("/agency-orders")
    public ResponseEntity<List<ReservationDTO>> getAgencyOrders(Principal principal) {
        return ResponseEntity.ok(reservationService.getReservationsByOrganizer(principal.getName()));
    }
}