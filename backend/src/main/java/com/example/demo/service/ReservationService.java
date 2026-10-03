package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.model.Reservation;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ReservationDTO createReservation(Long travelerId, Long tripId) {

        // 1. Cerchiamo il viaggiatore
        User traveler = userRepository.findById(travelerId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + travelerId));

        // 2. Verifiche di business sull'utente
        if (traveler.getRole() != User.Role.VIAGGIATORE) {
            throw new RuntimeException("Errore: Solo i viaggiatori possono effettuare prenotazioni!");
        }

        // 3. Cerchiamo il viaggio
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaggio non trovato con ID: " + tripId));

        // 4. Verifica posti disponibili
        if (trip.getAvailableSpots() <= 0) {
            throw new RuntimeException("Spiacenti, i posti per questo viaggio sono esauriti!");
        }

        // 5. Creiamo la prenotazione
        Reservation reservation = new Reservation();
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus(Reservation.Status.CONFIRMED); // Immaginiamo che sia confermata subito
        reservation.setTraveler(traveler);
        reservation.setTrip(trip);

        // 6. Riduciamo i posti disponibili del viaggio
        trip.setAvailableSpots(trip.getAvailableSpots() - 1);
        tripRepository.save(trip); // Aggiorniamo il viaggio

        // 7. Salviamo la prenotazione
        Reservation savedReservation = reservationRepository.save(reservation);

        // 8. Convertiamo in DTO
        ReservationDTO dto = modelMapper.map(savedReservation, ReservationDTO.class);
        dto.setTripTitle(trip.getTitle()); // Aggiungiamo il titolo a mano per comodità del frontend

        return dto;
    }

    /**
     * Recupera tutte le prenotazioni di un viaggiatore specifico
     */
    public List<ReservationDTO> getReservationsByTraveler(Long travelerId) {
        return reservationRepository.findByTravelerId(travelerId).stream()
                .map(reservation -> {
                    ReservationDTO dto = modelMapper.map(reservation, ReservationDTO.class);
                    dto.setTripTitle(reservation.getTrip().getTitle());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}