package com.example.demo.repository;

import com.example.demo.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Tutte le conversazioni del viaggiatore loggato
    List<ChatMessage> findByTravelerIdOrderByTimestampAsc(Long travelerId);

    // Tutte le conversazioni ricevute sui pacchetti dell'agenzia loggata
    List<ChatMessage> findByTripOrganizerIdOrderByTimestampAsc(Long organizerId);

    // Tutti i messaggi di una specifica stanza (Viaggio + Viaggiatore)
    List<ChatMessage> findByTripIdAndTravelerEmailIgnoreCaseOrderByTimestampAsc(Long tripId, String travelerEmail);

    List<ChatMessage> findByTripOrganizerIdAndTravelerEmailIgnoreCaseOrderByTimestampAsc(Long organizerId, String travelerEmail);
}