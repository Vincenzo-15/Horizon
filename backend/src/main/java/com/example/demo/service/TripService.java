package com.example.demo.service;

import com.example.demo.dto.TripDTO;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository; // Ci serve per cercare l'organizzatore
    private final ModelMapper modelMapper;

    /**
     * Crea un nuovo viaggio associandolo a un organizzatore
     */
    // IN TRIP SERVICE
    public TripDTO createTrip(Trip trip, String organizerEmail) {
        // 1. Cerchiamo l'utente tramite l'email presa dal Token, non dall'URL
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (organizer.getRole() != User.Role.ORGANIZZATORE) {
            throw new RuntimeException("Solo gli organizzatori possono creare un viaggio!");
        }

        trip.setOrganizer(organizer);
        Trip savedTrip = tripRepository.save(trip);
        return modelMapper.map(savedTrip, TripDTO.class);
    }

    /**
     * Recupera tutti i viaggi disponibili
     */
    public List<TripDTO> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(trip -> modelMapper.map(trip, TripDTO.class))
                .collect(Collectors.toList());
    }
}
