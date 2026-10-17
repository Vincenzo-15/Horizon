package com.example.demo.controller;

import com.example.demo.dto.TripDTO;
import com.example.demo.model.Trip;
import com.example.demo.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    /**
     * Endpoint per creare un viaggio.
     * Metodo HTTP: POST
     * URL: http://localhost:8080/api/trips/organizer/{organizerId}
     */

    @PostMapping("/create")
    public ResponseEntity<TripDTO> createTrip(@RequestBody Trip trip, java.security.Principal principal) {
        // principal.getName() contiene l'email dell'utente estratta in modo sicuro dal Token JWT
        TripDTO createdTrip = tripService.createTrip(trip, principal.getName());
        return new ResponseEntity<>(createdTrip, HttpStatus.CREATED);
    }

    @GetMapping("/my-trips")
    public ResponseEntity<List<TripDTO>> getMyTrips(Principal principal) {
        return ResponseEntity.ok(tripService.getTripsByOrganizer(principal.getName()));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<TripDTO>> getPopularTrips() {
        // In futuro qui potrai chiamare un metodo del Service che filtra per rating più alto.
        // Per ora usiamo getAllTrips, ma esponiamolo sulla rotta corretta.
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    /**
     * Endpoint per vedere tutti i viaggi.
     * Metodo HTTP: GET
     * URL: http://localhost:8080/api/trips
     */
    @GetMapping
    public ResponseEntity<List<TripDTO>> getAllTrips() {
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    // --- NUOVO: Dettaglio singolo viaggio ---
    @GetMapping("/{id}")
    public ResponseEntity<TripDTO> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    // --- NUOVO: Eliminazione viaggio da parte dell'agenzia ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id, java.security.Principal principal) {
        tripService.deleteTrip(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripDTO> updateTrip(
            @PathVariable Long id,
            @RequestBody Trip trip,
            Principal principal) {

        // principal.getName() garantisce che prendiamo l'email dal token dell'utente attualmente loggato
        TripDTO updatedTrip = tripService.updateTrip(id, trip, principal.getName());
        return ResponseEntity.ok(updatedTrip);
    }
}
