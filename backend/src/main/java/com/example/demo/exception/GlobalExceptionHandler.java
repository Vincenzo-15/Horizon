package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice // Indica che questa classe ascolta gli errori da TUTTI i controller
public class GlobalExceptionHandler {

    /**
     * Intercetta tutte le RuntimeException lanciate dai nostri Service
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {

        // Costruiamo il nostro oggetto di errore personalizzato
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(), // Restituiamo 400 Bad Request
                "Bad Request",
                ex.getMessage(), // Prende il messaggio esatto ("Utente non trovato", "Posti esauriti", ecc.)
                request.getDescription(false).replace("uri=", "") // Prende l'URL che ha causato l'errore
        );

        // Inviamo la risposta al client (Postman/Android)
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

}
