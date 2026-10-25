package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer availableSpots;

    @Column(nullable = false)
    private String location;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_images", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> imageUrls;

    // Valori di default espliciti anche a livello di database SQL
    @Column(nullable = false, columnDefinition = "double precision default 0.0")
    private Double averageRating = 0.0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer reviewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;
}