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
    private String ownerName;
    private String ownerEmail;
    private Set<Long> tripIds;
}
