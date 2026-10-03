package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public String login(String email, String password) {
        // 1. Cerchiamo l'utente
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenziali non valide"));

        // 2. Verifichiamo la password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Credenziali non valide");
        }

        // 3. Generiamo il token aggiungendo il ruolo come "claim" extra
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());

        return jwtService.generateToken(extraClaims, user);
    }
}