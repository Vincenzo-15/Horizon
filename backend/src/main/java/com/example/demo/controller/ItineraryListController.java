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

    @PostMapping("/create")
    public ResponseEntity<ItineraryListDTO> createList(
            @RequestParam String name,
            @RequestParam String visibility,
            Principal principal) {
        return new ResponseEntity<>(itineraryListService.createList(name, visibility, principal.getName()), HttpStatus.CREATED);
    }

    @PostMapping("/{listId}/add-trip/{tripId}")
    public ResponseEntity<ItineraryListDTO> addTripToList(@PathVariable Long listId, @PathVariable Long tripId) {
        return ResponseEntity.ok(itineraryListService.addTripToList(listId, tripId));
    }

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

    @PutMapping("/{listId}")
    public ResponseEntity<?> updateList(
            @PathVariable Long listId,
            @RequestParam String name,
            @RequestParam String visibility,
            Principal principal) {
        try {
            ItineraryListDTO updatedList = itineraryListService.updateList(listId, name, visibility, principal.getName());
            return ResponseEntity.ok(updatedList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-lists")
    public ResponseEntity<List<ItineraryListDTO>> getMyLists(Principal principal) {
        return ResponseEntity.ok(itineraryListService.getListsByTraveler(principal.getName()));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<ItineraryListDTO> getListDetails(@PathVariable Long listId) {
        return ResponseEntity.ok(itineraryListService.getListDetails(listId));
    }

    @GetMapping("/{listId}/trips")
    public ResponseEntity<List<com.example.demo.dto.TripDTO>> getTripsInList(@PathVariable Long listId) {
        return ResponseEntity.ok(itineraryListService.getTripsInList(listId));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<?> deleteList(@PathVariable Long listId, Principal principal) {
        try {
            itineraryListService.deleteList(listId, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}