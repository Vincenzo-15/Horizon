package com.example.demo.controller;

import com.example.demo.dto.ItineraryListDTO;
import com.example.demo.service.ItineraryListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class ItineraryListController {

    private final ItineraryListService itineraryListService;

    /**
     * Crea una nuova lista (es. "Vacanze Estive")
     * POST http://localhost:8080/api/lists/traveler/{travelerId}?name=Estate&visibility=PRIVATE
     */
    @PostMapping("/traveler/{travelerId}")
    public ResponseEntity<ItineraryListDTO> createList(
            @PathVariable Long travelerId,
            @RequestParam String name,
            @RequestParam String visibility) {

        return new ResponseEntity<>(itineraryListService.createList(name, visibility, travelerId), HttpStatus.CREATED);
    }

    /**
     * Aggiunge un viaggio specifico a una lista
     * PUT http://localhost:8080/api/lists/{listId}/trips/{tripId}
     */
    @PutMapping("/{listId}/trips/{tripId}")
    public ResponseEntity<ItineraryListDTO> addTripToList(
            @PathVariable Long listId,
            @PathVariable Long tripId) {

        return ResponseEntity.ok(itineraryListService.addTripToList(listId, tripId));
    }

    @GetMapping("/traveler/{travelerId}")
    public ResponseEntity<List<ItineraryListDTO>> getMyLists(@PathVariable Long travelerId) {
        return ResponseEntity.ok(itineraryListService.getListsByTraveler(travelerId));
    }
}