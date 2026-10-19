package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service // Diciamo a Spring che questa classe contiene la logica di business
@RequiredArgsConstructor // Lombok crea automaticamente il costruttore per iniettare i parametri final
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Crea un nuovo utente nel database
     */
    public UserDTO createUser(User user) {

        // 1. Controllo Business: Esiste già un utente con questa email?
        // (Questo controllo DEVE restare qui perché richiede di interrogare il Database)
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Esiste già un utente con questa email!");
        }

        if(user.getRole() == null) {
            user.setRole(User.Role.VIAGGIATORE); // Impostiamo un ruolo di default se non specificato
        }

        // 2. CRITTOGRAFIA (Ora la password ci arriva già controllata e sicura da @Valid)
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Salvataggio
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDTO.class);
    }

    /**
     * Cerca un utente per ID
     */
    public UserDTO getUserById(Long id) {
        // Cerchiamo l'utente. Se non esiste, lanciamo un'eccezione
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + id));

        return modelMapper.map(user, UserDTO.class);
    }

    /**
     * Recupera tutti gli utenti registrati
     */
    public List<UserDTO> getAllUsers() {
        // Recuperiamo tutti gli utenti, li trasformiamo in DTO uno per uno e li mettiamo in una lista
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
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

    /**
     * Modifica la password dell'utente loggato verificando quella attuale
     */
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

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Recupero password dalla schermata di Login tramite email registrata
     */
    @Transactional
    public void resetForgottenPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Nessun account trovato con questa email."));

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("La nuova password deve avere almeno 8 caratteri.");
        }

        userRepository.updateUserPassword(user.getEmail(), passwordEncoder.encode(newPassword));
    }
}