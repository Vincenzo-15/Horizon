package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    private final Map<String, PasswordResetToken> resetTokens = new ConcurrentHashMap<>();

    private static class PasswordResetToken {
        final String email;
        final LocalDateTime expiresAt;

        PasswordResetToken(String email, LocalDateTime expiresAt) {
            this.email = email;
            this.expiresAt = expiresAt;
        }
    }

    public UserDTO createUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Esiste già un utente con questa email!");
        }

        if (user.getRole() == null) {
            user.setRole(User.Role.VIAGGIATORE);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        return mapUserToDTO(savedUser);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + id));

        return mapUserToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO mapUserToDTO(User user) {
        UserDTO dto = modelMapper.map(user, UserDTO.class);
        dto.setTelefono(user.getTelefono());
        dto.setPartitaIva(user.getPartitaIva());
        dto.setRole(user.getRole().name());
        return dto;
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato."));
        return mapUserToDTO(user);
    }

    @Transactional
    public UserDTO updateCurrentUser(String email, String name, String telefono, String partitaIva) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato."));

        String finalName = (name != null && !name.isBlank()) ? name.trim() : user.getName();
        String finalPhone = (telefono != null && !telefono.isBlank()) ? telefono.trim() : null;
        String finalPiva = (user.getRole() == User.Role.ORGANIZZATORE && partitaIva != null && !partitaIva.isBlank())
                ? partitaIva.trim()
                : user.getPartitaIva();

        userRepository.updateProfileFields(email, finalName, finalPhone, finalPiva);

        User refreshedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato."));
        return mapUserToDTO(refreshedUser);
    }

    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato."));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("La password attuale inserita non è corretta.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("La nuova password deve contenere almeno 8 caratteri.");
        }

        userRepository.updateUserPassword(email, passwordEncoder.encode(newPassword));
    }

    @Transactional(readOnly = true)
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Nessun account registrato con questo indirizzo email."));

        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new PasswordResetToken(user.getEmail(), LocalDateTime.now().plusMinutes(15)));

        String resetLink = "http://localhost:8080/api/auth/reset-password-form?token=" + token;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setFrom("security@horizontravel.it");
            helper.setTo(user.getEmail());
            helper.setSubject("Horizon Travel - Reimposta la tua password");

            String htmlBody = """
                <div style="font-family:sans-serif;max-width:520px;margin:auto;padding:28px;background:#F4F6F5;border-radius:20px;">
                    <div style="background:#FFFFFF;padding:28px;border-radius:16px;">
                        <span style="background:#E6F7F0;color:#00AA6C;padding:4px 10px;border-radius:50px;font-size:11px;font-weight:bold;">
                            HORIZON TRAVEL • SICUREZZA
                        </span>
                        <h2 style="color:#1A1A1A;margin-top:14px;">Ciao %s,</h2>
                        <p style="color:#444;font-size:14px;line-height:1.6;">
                            Hai richiesto di reimpostare la password per l'account <strong>%s</strong>.
                            Clicca sul pulsante qui sotto per scegliere una nuova password (valido per 15 minuti):
                        </p>
                        <div style="text-align:center;margin:28px 0;">
                            <a href="%s" style="background:#00AA6C;color:#FFFFFF;text-decoration:none;padding:14px 26px;border-radius:12px;font-weight:bold;font-size:15px;display:inline-block;">
                                Reimposta la tua Password
                            </a>
                        </div>
                    </div>
                </div>
                """.formatted(user.getName(), user.getEmail(), resetLink);

            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("⚠️ Avviso SMTP: " + e.getMessage());
        }

        System.out.println("\n================================================================");
        System.out.println("📧 EMAIL DI RECUPERO INVIATA SU http://localhost:8025");
        System.out.println("👤 Destinatario: " + user.getName() + " <" + user.getEmail() + ">");
        System.out.println("🔗 Link: " + resetLink);
        System.out.println("================================================================\n");

        return resetLink;
    }

    public String getEmailForResetToken(String token) {
        PasswordResetToken data = resetTokens.get(token);
        if (data == null || LocalDateTime.now().isAfter(data.expiresAt)) {
            resetTokens.remove(token);
            throw new IllegalArgumentException("Il link di recupero è scaduto o non è più valido.");
        }
        return data.email;
    }

    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        String email = getEmailForResetToken(token);

        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$";
        if (newPassword == null || !newPassword.matches(passwordPattern)) {
            throw new IllegalArgumentException(
                    "La password deve avere almeno 8 caratteri, una maiuscola, un numero e un carattere speciale (!@#$%^&+=!)."
            );
        }

        userRepository.updateUserPassword(email, passwordEncoder.encode(newPassword));
        resetTokens.remove(token);
    }
}