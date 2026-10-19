package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.model.Notification;
import com.example.demo.model.Reservation;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.NotificationRepository;
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
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ReservationDTO createReservation(Long tripId, Integer spots, String travelerEmail) {

        User traveler = userRepository.findByEmail(travelerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato nel sistema."));

        if (traveler.getRole() != User.Role.VIAGGIATORE) {
            throw new IllegalArgumentException("Errore: Solo i viaggiatori possono effettuare prenotazioni.");
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Il viaggio selezionato non è più disponibile."));

        if (trip.getAvailableSpots() < spots) {
            throw new IllegalArgumentException("Spiacenti, i posti sono esauriti. Rimasti: " + trip.getAvailableSpots());
        }

        Reservation reservation = new Reservation();
        reservation.setReservationDate(LocalDateTime.now());
        reservation.setStatus(Reservation.Status.CONFIRMED);
        reservation.setTraveler(traveler);
        reservation.setTrip(trip);
        reservation.setBookedSpots(spots);

        double totale = trip.getPrice().doubleValue() * spots;
        reservation.setTotalPaid(totale);

        trip.setAvailableSpots(trip.getAvailableSpots() - spots);
        tripRepository.save(trip);

        Reservation savedReservation = reservationRepository.save(reservation);

        // 1. Notifica per l'Organizzatore (Agenzia che ha creato il viaggio/attività)
        if (trip.getOrganizer() != null) {
            String agencyTitle = "Nuova prenotazione ricevuta!";
            String agencyMsg = traveler.getName() + " ha prenotato " + spots +
                    " posto/i per \"" + trip.getTitle() + "\" (Totale: €" + totale + ").";
            notificationRepository.save(new Notification(agencyTitle, agencyMsg, trip.getOrganizer()));
        }

        // 2. Notifica di promemoria per il Viaggiatore
        String travelerTitle = "Prenotazione confermata!";
        String travelerMsg = "Hai prenotato " + spots + " posto/i per \"" + trip.getTitle() +
                "\". Puoi esportare il promemoria in formato calendario (.ics) dalla sezione Viaggi.";
        notificationRepository.save(new Notification(travelerTitle, travelerMsg, traveler));

        System.out.println("\n================================================");
        System.out.println("✅ NUOVA PRENOTAZIONE CONFERMATA!");
        System.out.println("📧 Inviata email di riepilogo a: " + traveler.getEmail());
        System.out.println("🧳 Pacchetto: " + trip.getTitle());
        System.out.println("🎟️ Posti acquistati: " + spots);
        System.out.println("💳 Totale pagato: " + totale + "€");
        System.out.println("================================================\n");

        ReservationDTO dto = modelMapper.map(savedReservation, ReservationDTO.class);
        dto.setTripTitle(trip.getTitle());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByTraveler(String travelerEmail) {
        User traveler = userRepository.findByEmail(travelerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        return reservationRepository.findByTravelerId(traveler.getId()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getReservationsByOrganizer(String organizerEmail) {
        return reservationRepository.findByTripOrganizerEmail(organizerEmail).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ReservationDTO convertToDTO(Reservation reservation) {
        ReservationDTO dto = modelMapper.map(reservation, ReservationDTO.class);
        dto.setTripTitle(reservation.getTrip().getTitle());
        return dto;
    }
}