package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "itinerary_lists")
@Getter
@Setter
@NoArgsConstructor
// Previene il crash (StackOverflowError) causato dal toString/equals di Lombok sui Set in relazioni ManyToMany
@EqualsAndHashCode(exclude = {"trips", "sharedUsers"})
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

    // La tabella di mezzo (JoinTable) per gestire gli utenti specifici con cui è condivisa
    @ManyToMany
    @JoinTable(
            name = "list_shared_users",
            joinColumns = @JoinColumn(name = "list_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> sharedUsers = new HashSet<>();

    public enum Visibility {
        PRIVATE,
        SHARED,
        PUBLIC
    }
}