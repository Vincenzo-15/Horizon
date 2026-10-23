package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ItineraryListRepository itineraryListRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Database già popolato
        }

        String defaultHash = passwordEncoder.encode("Horizon2026!");

        // 1. CREAZIONE UTENTI (2 Agenzie + 2 Viaggiatori)
        User agenziaItalia = new User();
        agenziaItalia.setName("Horizon Italia Grand Tours");
        agenziaItalia.setEmail("agenzia@horizon.it");
        agenziaItalia.setPassword(defaultHash);
        agenziaItalia.setRole(User.Role.ORGANIZZATORE);
        agenziaItalia.setPartitaIva("IT09876543210");
        agenziaItalia.setTelefono("+39 06 5544332");
        agenziaItalia = userRepository.save(agenziaItalia);

        User agenziaEuropa = new User();
        agenziaEuropa.setName("Alpine & World Experiences");
        agenziaEuropa.setEmail("alpi.tours@horizon.it");
        agenziaEuropa.setPassword(defaultHash);
        agenziaEuropa.setRole(User.Role.ORGANIZZATORE);
        agenziaEuropa.setPartitaIva("IT01234567890");
        agenziaEuropa.setTelefono("+39 02 9988776");
        agenziaEuropa = userRepository.save(agenziaEuropa);

        User mario = new User();
        mario.setName("Mario Rossi");
        mario.setEmail("mario@horizon.it");
        mario.setPassword(defaultHash);
        mario.setRole(User.Role.VIAGGIATORE);
        mario.setTelefono("+39 333 1122334");
        mario = userRepository.save(mario);

        User giulia = new User();
        giulia.setName("Giulia Bianchi");
        giulia.setEmail("giulia@horizon.it");
        giulia.setPassword(defaultHash);
        giulia.setRole(User.Role.VIAGGIATORE);
        giulia.setTelefono("+39 347 8899001");
        giulia = userRepository.save(giulia);

        // 2. CREAZIONE VIAGGI MULTI-GIORNO E ATTIVITÀ GIORNALIERE (Con Gallerie Multi-Foto)
        Trip romaWeekend = createTrip(
                "Roma Imperiale & Trastevere Autentica (4 Giorni)",
                "Un viaggio indimenticabile di 4 giorni nel cuore della Città Eterna. Include boutique hotel in centro storico, ingresso prioritario al Colosseo e Foro Romano, passeggiata serale a Trastevere e degustazione guidata della tradizione romana.",
                new BigDecimal("490.00"),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 15),
                12,
                "Roma",
                4.9,
                42,
                agenziaItalia,
                List.of(
                        "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1529260830199-42c24126f198?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1515542622106-78bda8ba0e5b?auto=format&fit=crop&w=1000&q=80"
                )
        );

        Trip romaTourColosseo = createTrip(
                "Attività: Tour Guidato Colosseo, Arena e Sotterranei",
                "Escursione giornaliera di 3 ore con archeologo ufficiale. Accesso diretto al piano dell'Arena dei Gladiatori, visita ai sotterranei e passeggiata panoramica sul Colle Palatino.",
                new BigDecimal("65.00"),
                LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 11, 14), // Stesso giorno = Attività
                4, // Pochi posti (mostra il badge 'In esaurimento' nella Console Agenzia)
                "Roma",
                4.8,
                89,
                agenziaItalia,
                List.of(
                        "https://images.unsplash.com/photo-1548585742-1df49e753a83?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1000&q=80"
                )
        );

        Trip losannaViaggio = createTrip(
                "Losanna, Lago di Ginevra & Vigneti di Lavaux UNESCO",
                "Esperienza alpina di 4 giorni affacciata sul Lago Lemano. Comprende soggiorno vista lago a Ouchy, ingresso al Museo Olimpico, trenino panoramico tra le terrazze vinicole di Lavaux (Patrimonio UNESCO) e crociera in battello storico.",
                new BigDecimal("640.00"),
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 23),
                10,
                "Losanna",
                4.9,
                31,
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1527668752968-14dc70a27c95?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1000&q=80"
                )
        );

        Trip losannaDegustazione = createTrip(
                "Escursione & Degustazione Vini a Lavaux con Crociera",
                "Attività giornaliera con partenza dal porto di Losanna-Ouchy. Navigazione sul Lago di Ginevra e degustazione guidata di Chasselas presso una cantina storica a picco sull'acqua.",
                new BigDecimal("85.00"),
                LocalDate.of(2026, 11, 21),
                LocalDate.of(2026, 11, 21),
                8,
                "Losanna",
                4.7,
                19,
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1527668752968-14dc70a27c95?auto=format&fit=crop&w=1000&q=80"
                )
        );

        Trip parigiViaggio = createTrip(
                "Parigi Bohémien: Montmartre, Louvre e Senna (5 Giorni)",
                "Scopri il fascino senza tempo della Ville Lumière: hotel 4 stelle nel quartiere Marais, ingresso riservato al Museo del Louvre, salita sulla Torre Eiffel al tramonto e cena romantica in battello sulla Senna.",
                new BigDecimal("720.00"),
                LocalDate.of(2026, 12, 4),
                LocalDate.of(2026, 12, 8),
                14,
                "Parigi",
                4.8,
                54,
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?auto=format&fit=crop&w=1000&q=80"
                )
        );

        Trip amalfiCostiera = createTrip(
                "Costiera Amalfitana, Positano & Capri in Barca (4 Giorni)",
                "Soggiorno panoramico ad Amalfi con escursione privata in gozzo sorrentino verso i Faraglioni di Capri, trekking guidato lungo il celebre Sentiero degli Dei e degustazione di limoncello artigianale.",
                new BigDecimal("580.00"),
                LocalDate.of(2026, 11, 26),
                LocalDate.of(2026, 11, 29),
                3, // In esaurimento
                "Amalfi",
                5.0,
                63,
                agenziaItalia,
                List.of(
                        "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&w=1000&q=80"
                )
        );

        Trip tokyoAvventura = createTrip(
                "Tokyo Futuristica, Kyoto & Monte Fuji (9 Giorni)",
                "Un'immersione totale nella cultura giapponese: dai templi storici di Asakusa all'incrocio di Shibuya, escursione panoramica al Monte Fuji e viaggio in Shinkansen verso i santuari di Kyoto.",
                new BigDecimal("1890.00"),
                LocalDate.of(2027, 1, 15),
                LocalDate.of(2027, 1, 24),
                9,
                "Tokyo",
                4.9,
                77,
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1000&q=80"
                )
        );

        // 3. CREAZIONE LISTE PREFERITI (Privata, Condivisa tra Mario e Giulia, Pubblica)
        ItineraryList listaCondivisa = new ItineraryList();
        listaCondivisa.setName("Estate & Weekend con Amici");
        listaCondivisa.setVisibility(ItineraryList.Visibility.SHARED);
        listaCondivisa.setTraveler(mario);
        listaCondivisa.getTrips().add(romaWeekend);
        listaCondivisa.getTrips().add(amalfiCostiera);
        listaCondivisa.getTrips().add(losannaDegustazione);
        listaCondivisa.getSharedUsers().add(giulia);
        itineraryListRepository.save(listaCondivisa);

        ItineraryList listaDiGiuliaConMario = new ItineraryList();
        listaDiGiuliaConMario.setName("Idee Svizzera & Francia");
        listaDiGiuliaConMario.setVisibility(ItineraryList.Visibility.SHARED);
        listaDiGiuliaConMario.setTraveler(giulia);
        listaDiGiuliaConMario.getTrips().add(losannaViaggio);
        listaDiGiuliaConMario.getTrips().add(parigiViaggio);
        listaDiGiuliaConMario.getSharedUsers().add(mario);
        itineraryListRepository.save(listaDiGiuliaConMario);

        ItineraryList listaPrivataMario = new ItineraryList();
        listaPrivataMario.setName("Il mio Sogno in Giappone");
        listaPrivataMario.setVisibility(ItineraryList.Visibility.PRIVATE);
        listaPrivataMario.setTraveler(mario);
        listaPrivataMario.getTrips().add(tokyoAvventura);
        itineraryListRepository.save(listaPrivataMario);

        // 4. CREAZIONE PRENOTAZIONI CONFERMATE
        Reservation res1 = new Reservation();
        res1.setReservationDate(LocalDateTime.now().minusDays(2));
        res1.setStatus(Reservation.Status.CONFIRMED);
        res1.setTraveler(mario);
        res1.setTrip(romaWeekend);
        res1.setBookedSpots(2);
        res1.setTotalPaid(980.0);
        reservationRepository.save(res1);

        Reservation res2 = new Reservation();
        res2.setReservationDate(LocalDateTime.now().minusDays(1));
        res2.setStatus(Reservation.Status.CONFIRMED);
        res2.setTraveler(giulia);
        res2.setTrip(romaTourColosseo);
        res2.setBookedSpots(3);
        res2.setTotalPaid(195.0);
        reservationRepository.save(res2);

        // 5. CREAZIONE NOTIFICHE REALI PER VIAGGIATORI E AGENZIA
        notificationRepository.save(new Notification(
                "Nuova lista condivisa",
                "Giulia Bianchi ha condiviso la lista 'Idee Svizzera & Francia' con te!",
                mario
        ));
        notificationRepository.save(new Notification(
                "Prenotazione confermata!",
                "Hai prenotato 2 posti per \"Roma Imperiale & Trastevere Autentica (4 Giorni)\". Puoi esportare il promemoria in formato calendario (.ics) dalla sezione Viaggi.",
                mario
        ));
        notificationRepository.save(new Notification(
                "Nuova lista condivisa",
                "Mario Rossi ha condiviso la lista 'Estate & Weekend con Amici' con te!",
                giulia
        ));
        notificationRepository.save(new Notification(
                "Nuova prenotazione ricevuta!",
                "Mario Rossi ha prenotato 2 posti per \"Roma Imperiale & Trastevere Autentica (4 Giorni)\" (Totale: €980.0).",
                agenziaItalia
        ));

        System.out.println("✅ DATABASE HORIZON POPOLATO CON SUCCESSO PER LA DEMO D'ESAME!");
    }

    private Trip createTrip(
            String title,
            String description,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            int spots,
            String location,
            double rating,
            int reviews,
            User organizer,
            List<String> images
    ) {
        Trip t = new Trip();
        t.setTitle(title);
        t.setDescription(description);
        t.setPrice(price);
        t.setStartDate(startDate);
        t.setEndDate(endDate);
        t.setAvailableSpots(spots);
        t.setLocation(location);
        t.setAverageRating(rating);
        t.setReviewCount(reviews);
        t.setOrganizer(organizer);
        t.setImageUrls(images);
        return tripRepository.save(t);
    }
}