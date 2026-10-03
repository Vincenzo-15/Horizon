package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class TripDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer availableSpots;
    private String location;
    private String imageUrl;
    private Double averageRating = 0.0;
    private Integer reviewCount = 0;
    private Long organizerId;
    private String organizerName;
}
