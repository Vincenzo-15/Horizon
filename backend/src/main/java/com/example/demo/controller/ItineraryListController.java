package com.example.demo.controller;

import com.example.demo.dto.ItineraryListDTO;
import com.example.demo.service.ItineraryListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ItineraryListController {

    private final ItineraryListService itineraryListService;

    // Crea lista (usa Principal invece dell'ID nell'URL per sicurezza)
    @PostMapping("/create")
    public ResponseEntity<ItineraryListDTO> createList(
            @RequestParam String name,
            @RequestParam String visibility,
            Principal principal) {

        // Dovrai aggiornare il tuo service per prendere l'email (principal.getName()) invece dell'ID!
        return new ResponseEntity<>(itineraryListService.createList(name, visibility, principal.getName()), HttpStatus.CREATED);
    }

    // Aggiungi viaggio alla lista
    @PostMapping("/{listId}/add-trip/{tripId}")
    public ResponseEntity<ItineraryListDTO> addTripToList(@PathVariable Long listId, @PathVariable Long tripId) {
        return ResponseEntity.ok(itineraryListService.addTripToList(listId, tripId));
    }

    // NUOVO: Condividi la lista con un amico (tramite email)
    @PostMapping("/{listId}/share")
    public ResponseEntity<?> shareList(
            @PathVariable Long listId,
            @RequestParam String friendEmail,
            Principal principal) {
        try {
            ItineraryListDTO updatedList = itineraryListService.shareListWithUser(listId, friendEmail, principal.getName());
            return ResponseEntity.ok(updatedList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Leggi le liste dell'utente
    @GetMapping("/my-lists")
    public ResponseEntity<List<ItineraryListDTO>> getMyLists(Principal principal) {
        // Dovrai aggiornare il tuo service per prendere l'email!
        return ResponseEntity.ok(itineraryListService.getListsByTraveler(principal.getName()));
    }

    // Recupera i dettagli di una singola lista
    @GetMapping("/{listId}")
    public ResponseEntity<ItineraryListDTO> getListDetails(@PathVariable Long listId) {
        return ResponseEntity.ok(itineraryListService.getListDetails(listId));
    }
}