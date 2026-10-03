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
     * Crea una nuova lista vuota per un viaggiatore
     */
    public ItineraryListDTO createList(String name, String visibility, Long travelerId) {
        User traveler = userRepository.findById(travelerId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

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
                .orElseThrow(() -> new RuntimeException("Lista non trovata"));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaggio non trovato"));

        list.getTrips().add(trip);
        // Grazie a @Transactional e JPA, il salvataggio nella tabella di join 'list_trips' è automatico

        return modelMapper.map(list, ItineraryListDTO.class);
    }

    /**
     * Recupera tutte le liste di un utente
     */
    public List<ItineraryListDTO> getListsByTraveler(Long travelerId) {
        return itineraryListRepository.findByTravelerId(travelerId).stream()
                .map(list -> modelMapper.map(list, ItineraryListDTO.class))
                .collect(Collectors.toList());
    }
}
