package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
public class ItineraryListDTO {
    private Long id;
    private String name;
    private String visibility;
    private Long travelerId;
    private Set<Long> tripIds; // Solo gli ID dei viaggi, non gli oggetti interi
}
