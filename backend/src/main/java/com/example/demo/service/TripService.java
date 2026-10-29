package com.example.demo.service;

import com.example.demo.dto.TripDTO;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
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
        // Mostra solo i pacchetti con almeno 1 posto libero!
        return tripRepository.findByAvailableSpotsGreaterThan(0).stream()
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

        // 1. Trova tutte le prenotazioni attive collegate a questo pacchetto
        List<Reservation> reservations = reservationRepository.findByTripId(id);

        // 2. Invia una notifica di rimborso e cancellazione a ciascun viaggiatore
        for (Reservation res : reservations) {
            if (res.getTraveler() != null) {
                Notification cancelNotif = new Notification(
                        "Viaggio Annullato & Rimborso Emesso",
                        "Il pacchetto \"" + trip.getTitle() + "\" è stato cancellato dall'agenzia. È stato disposto il rimborso integrale di €" + res.getTotalPaid() + " sul tuo metodo di pagamento.",
                        res.getTraveler()
                );
                notificationRepository.save(cancelNotif);
            }
        }

        // 3. Elimina le prenotazioni collegate per rispettare i vincoli del database
        reservationRepository.deleteAll(reservations);

        // 4. Elimina definitivamente il viaggio
        tripRepository.delete(trip);
    }

    /**
     * Modifica un viaggio esistente.
     * Verifica che chi lo modifica sia effettivamente il proprietario.
     */
    @Transactional
    public TripDTO updateTrip(Long id, Trip updatedTripData, String organizerEmail) {

        // 1. Cerco il viaggio nel database
        Trip existingTrip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Viaggio non trovato con ID: " + id));

        // 2. Controllo Sicurezza: Il viaggio appartiene a questo organizzatore?
        if (!existingTrip.getOrganizer().getEmail().equals(organizerEmail)) {
            throw new RuntimeException("Errore di Sicurezza: Non sei autorizzato a modificare questo pacchetto!");
        }

        // 3. Aggiorno solo i campi modificabili
        if (updatedTripData.getTitle() != null) {
            existingTrip.setTitle(updatedTripData.getTitle());
        }
        if (updatedTripData.getDescription() != null) {
            existingTrip.setDescription(updatedTripData.getDescription());
        }
        if (updatedTripData.getPrice() != null) {
            existingTrip.setPrice(updatedTripData.getPrice());
        }
        if (updatedTripData.getAvailableSpots() != null) {
            if(updatedTripData.getAvailableSpots() >= 0){
                existingTrip.setAvailableSpots(updatedTripData.getAvailableSpots());
            }
        }
        if (updatedTripData.getLocation() != null) {
            existingTrip.setLocation(updatedTripData.getLocation());
        }
        if (updatedTripData.getStartDate() != null) {
            existingTrip.setStartDate(updatedTripData.getStartDate());
        }
        if (updatedTripData.getEndDate() != null) {
            existingTrip.setEndDate(updatedTripData.getEndDate());
        }

        if (updatedTripData.getImageUrls() != null && !updatedTripData.getImageUrls().isEmpty()) {
            existingTrip.setImageUrls(updatedTripData.getImageUrls());
        }

        Trip savedTrip = tripRepository.save(existingTrip);
        return convertToDTO(savedTrip);
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