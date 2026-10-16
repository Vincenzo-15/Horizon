package com.example.demo.service;

import com.example.demo.dto.ItineraryListDTO;
import com.example.demo.model.ItineraryList;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.ItineraryListRepository;
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
public class ItineraryListService {

    private final ItineraryListRepository itineraryListRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ModelMapper modelMapper;

    /**
     * Crea una nuova lista vuota per un viaggiatore (usando l'email del token)
     */
    public ItineraryListDTO createList(String name, String visibility, String travelerEmail) {

        // Cerca l'utente tramite email, non più tramite ID!
        User traveler = userRepository.findByEmail(travelerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con questa email."));

        ItineraryList list = new ItineraryList();
        list.setName(name);
        list.setVisibility(ItineraryList.Visibility.valueOf(visibility.toUpperCase()));
        list.setTraveler(traveler);

        ItineraryList savedList = itineraryListRepository.save(list);
        return modelMapper.map(savedList, ItineraryListDTO.class);
    }

    /**
     * Aggiunge un viaggio a una lista esistente
     */
    @Transactional
    public ItineraryListDTO addTripToList(Long listId, Long tripId) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato"));

        list.getTrips().add(trip);
        // Grazie a @Transactional e JPA, il salvataggio nella tabella di join 'list_trips' è automatico

        return modelMapper.map(list, ItineraryListDTO.class);
    }

    /**
     * Recupera tutte le liste di un utente (cercando il suo ID tramite la sua email)
     */
    public List<ItineraryListDTO> getListsByTraveler(String travelerEmail) {

        User traveler = userRepository.findByEmail(travelerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato con questa email."));

        // Ora che abbiamo l'utente, usiamo il suo ID per cercare le liste
        return itineraryListRepository.findByTravelerId(traveler.getId()).stream()
                .map(list -> modelMapper.map(list, ItineraryListDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Condivide una lista con un utente specifico tramite la sua email
     */
    @Transactional
    public ItineraryListDTO shareListWithUser(Long listId, String friendEmail, String ownerEmail) {

        // 1. Troviamo la lista
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata"));

        // 2. Controllo di sicurezza: solo il creatore della lista può condividerla!
        if (!list.getTraveler().getEmail().equals(ownerEmail)) {
            throw new IllegalArgumentException("Non sei autorizzato a condividere questa lista.");
        }

        // 3. Troviamo l'amico tramite l'email
        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new IllegalArgumentException("Nessun utente trovato con questa email."));

        // 4. Se la lista era privata, la convertiamo in condivisa
        if (list.getVisibility() == ItineraryList.Visibility.PRIVATE) {
            list.setVisibility(ItineraryList.Visibility.SHARED);
        }

        // 5. Aggiungiamo l'amico alla lista degli accessi consentiti
        list.getSharedUsers().add(friend);

        // Salviamo su DB
        itineraryListRepository.save(list);

        return modelMapper.map(list, ItineraryListDTO.class);
    }

    /**
     * Recupera i dettagli di una singola lista tramite il suo ID
     */
    public ItineraryListDTO getListDetails(Long listId) {
        ItineraryList list = itineraryListRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista non trovata con ID: " + listId));

        return modelMapper.map(list, ItineraryListDTO.class);
    }
}