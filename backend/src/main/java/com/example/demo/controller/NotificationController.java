package com.example.demo.controller;

import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.TripDTO;
import com.example.demo.model.Notification;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ModelMapper modelMapper;

    // Chiave conversazione: "tripId_travelerEmail"
    private final Map<String, ConversationRoom> activeConversations = new ConcurrentHashMap<>();
    private final AtomicLong messageIdGenerator = new AtomicLong(100);

    private static class ConversationRoom {
        Long tripId;
        String tripTitle;
        String tripLocation;
        String agencyName;
        String agencyEmail;
        String travelerName;
        String travelerEmail;
        String lastMessage;
        String lastTimestamp;
        long lastUpdatedEpoch;
        int unreadForTraveler;
        int unreadForAgency;
        final List<ChatMessageDTO> messages = new CopyOnWriteArrayList<>();
    }

    private String buildRoomKey(Long tripId, String travelerEmail) {
        return tripId + "_" + travelerEmail.trim().toLowerCase(Locale.ROOT);
    }

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

    // 1. ELENCO CHAT ATTIVE (Stile WhatsApp: solo chat realmente avviate, ordinate per NON LETTI e poi per DATA ULTIMO MESSAGGIO)
    @GetMapping("/chat/conversations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatConversationDTO>> getMyConversations(Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        boolean isOrganizer = currentUser.getRole() == User.Role.ORGANIZZATORE;
        String myEmail = currentUser.getEmail().trim().toLowerCase(Locale.ROOT);

        List<ChatConversationDTO> result = activeConversations.values().stream()
                .filter(room -> {
                    if (isOrganizer) {
                        return room.agencyEmail.equalsIgnoreCase(myEmail);
                    } else {
                        return room.travelerEmail.equalsIgnoreCase(myEmail);
                    }
                })
                .map(room -> {
                    int unread = isOrganizer ? room.unreadForAgency : room.unreadForTraveler;
                    return new ChatConversationDTO(
                            room.tripId,
                            room.tripTitle,
                            room.tripLocation,
                            room.agencyName,
                            room.agencyEmail,
                            room.travelerName,
                            room.travelerEmail,
                            room.lastMessage,
                            room.lastTimestamp,
                            room.lastUpdatedEpoch,
                            unread
                    );
                })
                // Ordinamento WhatsApp: prima le chat con messaggi NON LETTI, poi in ordine di arrivo dell'ultimo messaggio
                .sorted(Comparator
                        .comparing((ChatConversationDTO c) -> c.getUnreadCount() > 0).reversed()
                        .thenComparing(Comparator.comparingLong(ChatConversationDTO::getLastUpdatedEpoch).reversed())
                )
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // 2. APRE UNA CHAT E SEGNA AUTOMATICAMENTE I MESSAGGI COME LETTI
    @GetMapping("/chat/{tripId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatMessageDTO>> getChatMessages(
            @PathVariable Long tripId,
            @RequestParam(required = false) String travelerEmail,
            Principal principal
    ) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        boolean isOrganizer = currentUser.getRole() == User.Role.ORGANIZZATORE;
        String targetTravelerEmail = (isOrganizer && travelerEmail != null && !travelerEmail.isBlank())
                ? travelerEmail.trim().toLowerCase(Locale.ROOT)
                : currentUser.getEmail().trim().toLowerCase(Locale.ROOT);

        String key = buildRoomKey(tripId, targetTravelerEmail);
        ConversationRoom room = activeConversations.get(key);
        if (room == null) {
            return ResponseEntity.ok(List.of());
        }

        // Aprendo la chat, azzera i messaggi non letti per chi sta leggendo
        if (isOrganizer) {
            room.unreadForAgency = 0;
        } else {
            room.unreadForTraveler = 0;
        }

        return ResponseEntity.ok(room.messages);
    }

    // 3. INVIA UN MESSAGGIO (Crea la chat se è il primo contatto dalla pagina Dettagli Viaggio)
    @PostMapping("/chat/{tripId}")
    @Transactional
    public ResponseEntity<ChatMessageDTO> sendChatMessage(
            @PathVariable Long tripId,
            @RequestBody SendChatMessageRequest request,
            Principal principal
    ) {
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaggio non trovato"));

        String cleanText = request.getMessage() != null ? request.getMessage().trim() : "";
        if (cleanText.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean isOrganizer = sender.getRole() == User.Role.ORGANIZZATORE;
        String targetTravelerEmail;
        String targetTravelerName;

        if (isOrganizer) {
            targetTravelerEmail = (request.getTravelerEmail() != null && !request.getTravelerEmail().isBlank())
                    ? request.getTravelerEmail().trim().toLowerCase(Locale.ROOT)
                    : "";
            User trav = userRepository.findByEmail(targetTravelerEmail).orElse(null);
            targetTravelerName = trav != null ? trav.getName() : targetTravelerEmail;
        } else {
            targetTravelerEmail = sender.getEmail().trim().toLowerCase(Locale.ROOT);
            targetTravelerName = sender.getName();
        }

        String agencyName = trip.getOrganizer() != null ? trip.getOrganizer().getName() : "Agenzia Partner Horizon";
        String agencyEmail = trip.getOrganizer() != null ? trip.getOrganizer().getEmail() : "agenzia@horizon.it";

        String key = buildRoomKey(tripId, targetTravelerEmail);
        ConversationRoom room = activeConversations.computeIfAbsent(key, k -> {
            ConversationRoom r = new ConversationRoom();
            r.tripId = trip.getId();
            r.tripTitle = trip.getTitle();
            r.tripLocation = trip.getLocation() != null ? trip.getLocation() : "";
            r.agencyName = agencyName;
            r.agencyEmail = agencyEmail;
            r.travelerName = targetTravelerName;
            r.travelerEmail = targetTravelerEmail;
            return r;
        });

        String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        ChatMessageDTO newMsg = new ChatMessageDTO(
                messageIdGenerator.incrementAndGet(),
                tripId,
                sender.getName(),
                sender.getEmail(),
                sender.getRole().name(),
                cleanText,
                timeNow
        );

        room.messages.add(newMsg);
        room.lastMessage = cleanText;
        room.lastTimestamp = timeNow;
        room.lastUpdatedEpoch = System.currentTimeMillis();

        if (isOrganizer) {
            room.unreadForTraveler += 1;
            room.unreadForAgency = 0;
            userRepository.findByEmail(targetTravelerEmail).ifPresent(travelerUser ->
                    notificationRepository.save(new Notification(
                            "Nuova risposta da " + agencyName,
                            "\"" + cleanText + "\" (" + trip.getTitle() + ")",
                            travelerUser
                    ))
            );
        } else {
            room.unreadForAgency += 1;
            room.unreadForTraveler = 0;
            if (trip.getOrganizer() != null) {
                notificationRepository.save(new Notification(
                        "Nuovo messaggio in Chat da " + sender.getName(),
                        "\"" + cleanText + "\" (" + trip.getTitle() + ")",
                        trip.getOrganizer()
                ));
            }
        }

        return ResponseEntity.ok(newMsg);
    }

    // 4. Salva una nuova recensione reale nel DB
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

    @Data
    public static class SendChatMessageRequest {
        private String message;
        private String travelerEmail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDTO {
        private Long id;
        private Long tripId;
        private String senderName;
        private String senderEmail;
        private String senderRole;
        private String message;
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatConversationDTO {
        private Long tripId;
        private String tripTitle;
        private String tripLocation;
        private String agencyName;
        private String agencyEmail;
        private String travelerName;
        private String travelerEmail;
        private String lastMessage;
        private String lastTimestamp;
        private long lastUpdatedEpoch;
        private int unreadCount;
    }
}