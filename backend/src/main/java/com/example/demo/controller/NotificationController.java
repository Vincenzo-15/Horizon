package com.example.demo.controller;

import com.example.demo.dto.NotificationDTO;
import com.example.demo.dto.TripDTO;
import com.example.demo.model.ChatMessage;
import com.example.demo.model.Notification;
import com.example.demo.model.Trip;
import com.example.demo.model.User;
import com.example.demo.repository.ChatMessageRepository;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ModelMapper modelMapper;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

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

    // 1. ELENCO CHAT ATTIVE SALVATE SU POSTGRESQL (ordinate per NON LETTI e poi per DATA ULTIMO MESSAGGIO)
    @GetMapping("/chat/conversations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ChatConversationDTO>> getMyConversations(Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        boolean isOrganizer = currentUser.getRole() == User.Role.ORGANIZZATORE;

        List<ChatMessage> allMessages = isOrganizer
                ? chatMessageRepository.findByTripOrganizerIdOrderByTimestampAsc(currentUser.getId())
                : chatMessageRepository.findByTravelerIdOrderByTimestampAsc(currentUser.getId());

        // Raggruppa i messaggi del DB per conversazione
        Map<String, List<ChatMessage>> grouped = new LinkedHashMap<>();
        for (ChatMessage msg : allMessages) {
            String key = msg.getTrip().getId() + "_" + msg.getTraveler().getId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
        }

        List<ChatConversationDTO> result = new ArrayList<>();
        for (List<ChatMessage> roomMessages : grouped.values()) {
            if (roomMessages.isEmpty()) continue;

            ChatMessage lastMsg = roomMessages.get(roomMessages.size() - 1);
            Trip trip = lastMsg.getTrip();
            User traveler = lastMsg.getTraveler();
            User organizer = trip.getOrganizer();

            String agencyName = organizer != null ? organizer.getName() : "Agenzia Partner Horizon";
            String agencyEmail = organizer != null ? organizer.getEmail() : "agenzia@horizon.it";

            // Conta i messaggi non letti inviati dall'altra parte
            int unreadCount = (int) roomMessages.stream()
                    .filter(m -> !m.isRead() && !m.getSender().getId().equals(currentUser.getId()))
                    .count();

            long epochMillis = lastMsg.getTimestamp()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            result.add(new ChatConversationDTO(
                    trip.getId(),
                    trip.getTitle(),
                    trip.getLocation() != null ? trip.getLocation() : "",
                    agencyName,
                    agencyEmail,
                    traveler.getName(),
                    traveler.getEmail(),
                    lastMsg.getMessage(),
                    lastMsg.getTimestamp().format(TIME_FMT),
                    epochMillis,
                    unreadCount
            ));
        }

        // Prima le chat con messaggi NON LETTI, poi in ordine di arrivo dell'ultimo messaggio
        result.sort(Comparator
                .comparing((ChatConversationDTO c) -> c.getUnreadCount() > 0).reversed()
                .thenComparing(Comparator.comparingLong(ChatConversationDTO::getLastUpdatedEpoch).reversed())
        );

        return ResponseEntity.ok(result);
    }

    // 2. APRE UNA CHAT DAL DB E SEGNA AUTOMATICAMENTE I MESSAGGI COME LETTI SU POSTGRESQL
    @GetMapping("/chat/{tripId}")
    @Transactional
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

        List<ChatMessage> roomMessages = chatMessageRepository
                .findByTripIdAndTravelerEmailIgnoreCaseOrderByTimestampAsc(tripId, targetTravelerEmail);

        // Segna come letti nel database i messaggi ricevuti dall'interlocutore
        List<ChatMessage> unreadToUpdate = new ArrayList<>();
        for (ChatMessage msg : roomMessages) {
            if (!msg.isRead() && !msg.getSender().getId().equals(currentUser.getId())) {
                msg.setRead(true);
                unreadToUpdate.add(msg);
            }
        }
        if (!unreadToUpdate.isEmpty()) {
            chatMessageRepository.saveAll(unreadToUpdate);
        }

        List<ChatMessageDTO> dtos = roomMessages.stream()
                .map(m -> new ChatMessageDTO(
                        m.getId(),
                        m.getTrip().getId(),
                        m.getSender().getName(),
                        m.getSender().getEmail(),
                        m.getSender().getRole().name(),
                        m.getMessage(),
                        m.getTimestamp().format(TIME_FMT)
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // 3. INVIA UN MESSAGGIO E LO SALVA PERMANENTEMENTE SU POSTGRESQL
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
        User travelerUser;

        if (isOrganizer) {
            String targetEmail = request.getTravelerEmail() != null ? request.getTravelerEmail().trim() : "";
            travelerUser = userRepository.findByEmail(targetEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Viaggiatore non trovato: " + targetEmail));
        } else {
            travelerUser = sender;
        }

        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setTrip(trip);
        chatMsg.setTraveler(travelerUser);
        chatMsg.setSender(sender);
        chatMsg.setMessage(cleanText);
        chatMsg.setTimestamp(LocalDateTime.now());
        chatMsg.setRead(false);

        ChatMessage savedMsg = chatMessageRepository.save(chatMsg);

        String agencyName = trip.getOrganizer() != null ? trip.getOrganizer().getName() : "Agenzia Partner Horizon";

        if (isOrganizer) {
            notificationRepository.save(new Notification(
                    "Nuova risposta da " + agencyName,
                    "\"" + cleanText + "\" (" + trip.getTitle() + ")",
                    travelerUser
            ));
        } else if (trip.getOrganizer() != null) {
            notificationRepository.save(new Notification(
                    "Nuovo messaggio in Chat da " + sender.getName(),
                    "\"" + cleanText + "\" (" + trip.getTitle() + ")",
                    trip.getOrganizer()
            ));
        }

        return ResponseEntity.ok(new ChatMessageDTO(
                savedMsg.getId(),
                tripId,
                sender.getName(),
                sender.getEmail(),
                sender.getRole().name(),
                savedMsg.getMessage(),
                savedMsg.getTimestamp().format(TIME_FMT)
        ));
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