package com.example.demo.controller;

import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.TripDTO;
import com.example.demo.model.Notification;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByTimestampDesc(user.getId());

        List<NotificationDTO> dtos = notifications.stream()
                .map(n -> modelMapper.map(n, NotificationDTO.class))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Salva una nuova recensione reale nel DB e ricalcola averageRating e reviewCount del pacchetto
    @PostMapping("/review/{tripId}")
    @Transactional
    public ResponseEntity<TripDTO> addReviewToTrip(
            @PathVariable Long tripId,
            @RequestBody CreateReviewRequest request,
            Principal principal
    ) {
        User traveler = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato"));

        int safeRating = Math.max(1, Math.min(5, request.getRating()));
        String safeComment = (request.getComment() != null ? request.getComment().trim() : "")
                .replace("||", " ")
                .replace("\n", " ");
        String dateFormatted = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALY));

        String newReviewBlock = "@@REVIEW@@" + traveler.getName() + "||" + safeRating + "||" + dateFormatted + "||" + safeComment;

        String currentDesc = trip.getDescription() != null ? trip.getDescription() : "";
        String updatedDesc = currentDesc + "\n" + newReviewBlock;
        trip.setDescription(updatedDesc);

        // Ricalcola media reale e conteggio reale da tutti i blocchi @@REVIEW@@ presenti nel DB
        List<Integer> allRatings = new ArrayList<>();
        String[] blocks = updatedDesc.split("@@REVIEW@@");
        for (int i = 1; i < blocks.length; i++) {
            String[] fields = blocks[i].trim().split("\\|\\|");
            if (fields.length >= 2) {
                try {
                    allRatings.add(Integer.parseInt(fields[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        int totalReviews = allRatings.size();
        double avg = allRatings.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double roundedAvg = Math.round(avg * 10.0) / 10.0;

        trip.setReviewCount(totalReviews);
        trip.setAverageRating(roundedAvg);
        Trip savedTrip = tripRepository.save(trip);

        if (trip.getOrganizer() != null) {
            notificationRepository.save(new Notification(
                    "Nuova recensione ricevuta (" + safeRating + "★)",
                    traveler.getName() + " ha lasciato una recensione su \"" + trip.getTitle() + "\".",
                    trip.getOrganizer()
            ));
        }

        TripDTO dto = modelMapper.map(savedTrip, TripDTO.class);
        if (savedTrip.getOrganizer() != null) {
            dto.setOrganizerName(savedTrip.getOrganizer().getName());
        }
        return ResponseEntity.ok(dto);
    }

    @Data
    public static class CreateReviewRequest {
        private int rating;
        private String comment;
    }
}