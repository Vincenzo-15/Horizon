package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "itinerary_lists")
@Data
@NoArgsConstructor
public class ItineraryList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    // Relazione: Molte liste create da un solo Viaggiatore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveler_id", nullable = false)
    private User traveler;

    // Relazione Many-to-Many: Una lista ha molti viaggi, un viaggio sta in molte liste
    @ManyToMany
    @JoinTable(
            name = "list_trips",
            joinColumns = @JoinColumn(name = "list_id"),
            inverseJoinColumns = @JoinColumn(name = "trip_id")
    )
    private Set<Trip> trips = new HashSet<>();

    public enum Visibility {
        PRIVATE,
        SHARED,
        PUBLIC
    }
}