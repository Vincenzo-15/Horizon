package com.example.demo.service;

import com.example.demo.dto.TripDTO;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
// --- IMPORT CORRETTO DI SPRING BOOT ---
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public TripDTO createTrip(Trip trip, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        if (organizer.getRole() != User.Role.ORGANIZZATORE) {
            throw new RuntimeException("Solo gli organizzatori possono creare un viaggio!");
        }

        trip.setOrganizer(organizer);
        Trip savedTrip = tripRepository.save(trip);
        return convertToDTO(savedTrip);
    }

    @Transactional(readOnly = true)
    public List<TripDTO> getAllTrips() {
        return tripRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripDTO> getTripsByOrganizer(String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new RuntimeException("Organizzatore non trovato"));

        return tripRepository.findByOrganizerId(organizer.getId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TripDTO getTripById(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Viaggio non trovato con ID: " + id));
        return convertToDTO(trip);
    }

    @Transactional
    public void deleteTrip(Long id, String organizerEmail) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Viaggio non trovato"));

        if (!trip.getOrganizer().getEmail().equals(organizerEmail)) {
            throw new RuntimeException("Non sei autorizzato a eliminare questo pacchetto!");
        }

        tripRepository.delete(trip);
    }

    private TripDTO convertToDTO(Trip trip) {
        TripDTO dto = modelMapper.map(trip, TripDTO.class);
        if (trip.getOrganizer() != null) {
            dto.setOrganizerId(trip.getOrganizer().getId());
            dto.setOrganizerName(trip.getOrganizer().getName());
        }
        return dto;
    }
}