package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReservationDTO {
    private Long id;
    private LocalDateTime reservationDate;
    private String status;
    private Long travelerId;
    private Long tripId;
    private String tripTitle; // Comodo per l'app Android da mostrare a schermo
}
