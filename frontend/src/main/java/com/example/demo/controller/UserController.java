package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Indica che questa classe gestisce API REST e restituisce dati in formato JSON
@RequestMapping("/api/users") // Tutte le rotte di questa classe inizieranno con http://localhost:8080/api/users
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Endpoint per ottenere la lista di tutti gli utenti.
     * Metodo HTTP: GET
     * URL: http://localhost:8080/api/users
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Endpoint per cercare un utente specifico tramite il suo ID.
     * Metodo HTTP: GET
     * URL: http://localhost:8080/api/users/1 (dove 1 è l'ID)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        // @PathVariable estrae l'ID dall'URL e lo passa al metodo
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
