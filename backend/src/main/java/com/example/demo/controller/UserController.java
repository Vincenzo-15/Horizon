package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // 1. Legge i dati reali dell'utente loggato
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyProfile(Principal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getName()));
    }

    // 2. Aggiorna i dati dell'account (Nome, Telefono, Partita IVA)
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            Principal principal) {
        try {
            UserDTO updated = userService.updateCurrentUser(
                    principal.getName(),
                    request.getName(),
                    request.getTelefono(),
                    request.getPartitaIva()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. Aggiorna la password dell'utente
    @PutMapping("/me/password")
    public ResponseEntity<?> changeMyPassword(
            @RequestBody ChangePasswordRequest request,
            Principal principal) {
        try {
            userService.changePassword(
                    principal.getName(),
                    request.getOldPassword(),
                    request.getNewPassword()
            );
            return ResponseEntity.ok(Map.of("message", "Password aggiornata con successo"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class UpdateProfileRequest {
        private String name;
        private String telefono;
        private String partitaIva;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}