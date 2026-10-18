package com.example.demo.service;

import com.example.demo.dto.ItineraryListDTO;
import com.example.demo.model.ItineraryList;
import com.example.demo.model.Notification;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.ItineraryListRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryListService {

    private final ItineraryListRepository itineraryListRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    /**
     * Converte l'entità in DTO includendo nome, email del creatore e ID dei viaggi
     */
    private ItineraryListDTO convertToDTO(ItineraryList list) {
        ItineraryListDTO dto = modelMapper.map(list, ItineraryListDTO.class);
        if (list.getTraveler() != null) {
            dto.setTravelerId(list.getTraveler().getId());
            dto.setOwnerName(list.getTraveler().getName());
            dto.setOwnerEmail(list.getTraveler().getEmail());
        }
        if (list.getTrips() != null) {
            Set<Long> ids = list.getTrips().stream()
                    .map(Trip::getId)
                    .collect(Collectors.toSet());
            dto.setTripIds(ids);
        }
        return dto;
    }

    @Transactional
    public ItineraryListDTO createList(String name, String visibility, String travelerEmail) {
        User traveler = userRepository.findByEmail(travelerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con questa email."));

        ItineraryList list = new ItineraryList();
        list.setName(name);
        list.setVisibility(ItineraryList.Visibility.valueOf(visibility.toUpperCase()));
        list.setTraveler(traveler);

        ItineraryList savedList = itineraryListRepository.save(list);
        return convertToDTO(savedList);
    }

    @Transactional
    public ItineraryListDTO addTripToList(Long listId, Long tripId) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato"));

        list.getTrips().add(trip);
        return convertToDTO(list);
    }

    @Transactional(readOnly = true)
    public List<ItineraryListDTO> getListsByTraveler(String travelerEmail) {
        User traveler = userRepository.findByEmail(travelerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con questa email."));

        List<ItineraryList> ownedLists = itineraryListRepository.findByTravelerId(traveler.getId());
        List<ItineraryList> sharedLists = itineraryListRepository.findListsSharedWithUser(traveler.getId())
                .stream()
                .filter(l -> l.getVisibility() != ItineraryList.Visibility.PRIVATE)
                .collect(Collectors.toList());

        java.util.Set<ItineraryList> allLists = new java.util.HashSet<>();
        allLists.addAll(ownedLists);
        allLists.addAll(sharedLists);

        return allLists.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItineraryListDTO shareListWithUser(Long listId, String friendEmail, String ownerEmail) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        if (!list.getTraveler().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new IllegalArgumentException("Solo il creatore può condividere questa lista.");
        }

        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new IllegalArgumentException("Nessun utente trovato con questa email."));

        if (list.getVisibility() == ItineraryList.Visibility.PRIVATE) {
            list.setVisibility(ItineraryList.Visibility.SHARED);
        }

        list.getSharedUsers().add(friend);
        itineraryListRepository.save(list);

        // --- NUOVO: Creazione della Notifica ---
        String notificationTitle = "Nuova lista condivisa";
        String notificationMessage = list.getTraveler().getName() + " ha condiviso la lista '" + list.getName() + "' con te!";
        Notification notification = new Notification(notificationTitle, notificationMessage, friend);
        notificationRepository.save(notification); // Salva la notifica nel DB
        // ---------------------------------------

        return convertToDTO(list);
    }

    /**
     * Modifica nome e visibilità della lista.
     * Se la lista passa a PRIVATE, svuota sharedUsers così gli amici non la vedono più.
     */
    @Transactional
    public ItineraryListDTO updateList(Long listId, String newName, String newVisibility, String requesterEmail) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        if (!list.getTraveler().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new IllegalArgumentException("Non sei autorizzato a modificare una lista che non hai creato tu.");
        }

        if (newName != null && !newName.isBlank()) {
            list.setName(newName.trim());
        }

        ItineraryList.Visibility targetVisibility = ItineraryList.Visibility.valueOf(newVisibility.toUpperCase());
        list.setVisibility(targetVisibility);

        // Se torna PRIVATA, rimuoviamo tutti gli utenti con cui era stata condivisa
        if (targetVisibility == ItineraryList.Visibility.PRIVATE) {
            list.getSharedUsers().clear();
        }

        ItineraryList saved = itineraryListRepository.save(list);
        return convertToDTO(saved);
    }

    /**
     * Elimina definitivamente la lista (rimuovendola anche dalla vista degli amici condivisi)
     */
    @Transactional
    public void deleteList(Long listId, String requesterEmail) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        if (!list.getTraveler().getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new IllegalArgumentException("Non sei autorizzato a eliminare una lista che non hai creato tu.");
        }

        list.getSharedUsers().clear();
        list.getTrips().clear();
        itineraryListRepository.delete(list);
    }

    @Transactional(readOnly = true)
    public List<com.example.demo.dto.TripDTO> getTripsInList(Long listId) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        return list.getTrips().stream()
                .map(trip -> modelMapper.map(trip, com.example.demo.dto.TripDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ItineraryListDTO getListDetails(Long listId) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata con ID: " + listId));

        return convertToDTO(list);
    }
}