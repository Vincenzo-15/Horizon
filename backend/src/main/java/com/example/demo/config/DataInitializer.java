package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.AllArgsConstructor;
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

    @AllArgsConstructor
    private static class SeedReview {
        final String author;
        final int rating;
        final String date;
        final String comment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        String defaultHash = passwordEncoder.encode("Horizon2026!");

        // 1. CREAZIONE UTENTI
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

        // 2. CREAZIONE VIAGGI E ATTIVITÀ CON TAPPE E RECENSIONI REALI SINCRONIZZATE AL 100%
        Trip romaWeekend = createTrip(
                "Roma Imperiale & Trastevere Autentica (4 Giorni)",
                "Un viaggio indimenticabile di 4 giorni nel cuore della Città Eterna tra archeologia, vicoli storici e sapori autentici romani." +
                        "\n@@STEP@@Giorno 1: Arrivo a Roma & Passeggiata Barocca||Giorno 1||🏨 Boutique Hotel 4* incluso||Check-in in hotel nel centro storico, briefing di benvenuto con il coordinatore e tour serale tra Piazza Navona, Pantheon e Fontana di Trevi." +
                        "\n@@STEP@@Giorno 2: Colosseo, Foro Romano e Colle Palatino||Giorno 2||🎟️ Biglietto salta-fila incluso||Mattinata dedicata all'antica Roma con archeologo privato: accesso prioritario all'Anfiteatro Flavio, Via Sacra e vista panoramica dal Palatino." +
                        "\n@@STEP@@Giorno 3: Musei Vaticani, Cappella Sistina & Trastevere||Giorno 3||🍷 Cena degustazione inclusa||Visita guidata della Basilica di San Pietro e dei Musei Vaticani. In serata, tour enogastronomico tra le botteghe storiche di Trastevere." +
                        "\n@@STEP@@Giorno 4: Giardino degli Aranci, Aventino e Rientro||Giorno 4||📸 Sosta panoramica||Mattinata rilassante sul Colle Aventino con vista dalla celebre serratura del Priorato di Malta, tempo libero per lo shopping e partenza.",
                new BigDecimal("490.00"),
                LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 15),
                12,
                "Roma",
                agenziaItalia,
                List.of(
                        "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1529260830199-42c24126f198?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1515542622106-78bda8ba0e5b?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Mario Rossi", 5, "18 Ott 2026", "Organizzazione impeccabile! L'hotel in centro e la cena a Trastevere hanno superato ogni aspettativa."),
                        new SeedReview("Giulia Bianchi", 5, "12 Ott 2026", "Archeologo preparatissimo durante la visita al Colosseo e Foro Romano. Consigliatissimo!"),
                        new SeedReview("Lorenzo V.", 4, "04 Ott 2026", "Bellissimo itinerario di 4 giorni, ritmi ben bilanciati e ottima assistenza dell'agenzia.")
                )
        );

        Trip romaTourColosseo = createTrip(
                "Attività: Tour Guidato Colosseo, Arena e Sotterranei",
                "Escursione giornaliera di 3 ore con archeologo ufficiale. Accesso diretto al piano dell'Arena dei Gladiatori, visita ai sotterranei e passeggiata sul Colle Palatino." +
                        "\n@@STEP@@Arco di Costantino: Accoglienza e Check-in||20 min||📍 Incontro con guida e radio||Incontro presso l'Arco di Costantino, consegna delle radioguide professionali e introduzione storica alla Roma Imperiale." +
                        "\n@@STEP@@Porta Libitinaria e Piano dell'Arena dei Gladiatori||50 min||🎟️ Accesso esclusivo Arena incluso||Ingresso riservato dalla Porta dei Gladiatori direttamente sul pavimento ligneo dell'Arena, con vista a 360 gradi sulle gradinate." +
                        "\n@@STEP@@Sotterranei del Colosseo (Ipogeo)||45 min||✨ Area riservata inclusa||Discesa nei corridoi sotterranei dove attendevano gladiatori e scenografie meccaniche prima degli spettacoli." +
                        "\n@@STEP@@Colle Palatino e Foro Romano||45 min||🎟️ Ingresso prioritario incluso||Salita panoramica sul Colle Palatino tra i resti dei palazzi imperiali e conclusione lungo la Via Sacra nel Foro Romano.",
                new BigDecimal("65.00"),
                LocalDate.of(2026, 11, 14),
                LocalDate.of(2026, 11, 14),
                4,
                "Roma",
                agenziaItalia,
                List.of(
                        "https://images.unsplash.com/photo-1548585742-1df49e753a83?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Giulia Bianchi", 5, "20 Ott 2026", "Entrare direttamente sull'Arena dei Gladiatori e visitare i sotterranei senza fila è stato emozionante!"),
                        new SeedReview("Matteo S.", 5, "15 Ott 2026", "Guida appassionata e competente, le 3 ore sono volate."),
                        new SeedReview("Chiara F.", 5, "09 Ott 2026", "Biglietti salta-fila utilissimi, tappa sul Colle Palatino davvero suggestiva."),
                        new SeedReview("Davide P.", 4, "01 Ott 2026", "Ottimo tour giornaliero, punto d'incontro facile da trovare vicino all'Arco di Costantino.")
                )
        );

        Trip losannaViaggio = createTrip(
                "Losanna, Lago di Ginevra & Vigneti di Lavaux UNESCO",
                "Esperienza alpina di 4 giorni affacciata sul Lago Lemano tra il centro storico medievale, il Museo Olimpico e i vigneti terrazzati patrimonio UNESCO." +
                        "\n@@STEP@@Giorno 1: Porto di Ouchy & Lungolago sul Lemano||Giorno 1||🏨 Hotel vista lago incluso||Sistemazione a Ouchy, passeggiata al tramonto sul lungolago con vista sulle Alpi francesi e cena di benvenuto svizzera." +
                        "\n@@STEP@@Giorno 2: Cité Medievale, Cattedrale & Museo Olimpico||Giorno 2||🎟️ Biglietto Museo Olimpico incluso||Esplorazione della Cattedrale gotica di Notre-Dame, salita degli scalini storici del mercato e pomeriggio interattivo al Museo Olimpico." +
                        "\n@@STEP@@Giorno 3: Trenino dei Vigneti di Lavaux & Cantine||Giorno 3||🍷 Degustazione 4 calici inclusa||Escursione tra i terrazzamenti vitivinicoli di Lavaux (UNESCO) a picco sul lago con degustazione in cantina storica a Epesses." +
                        "\n@@STEP@@Giorno 4: Crociera in Battello Belle Époque e Rientro||Giorno 4||⛴️ Crociera storica inclusa||Navigazione panoramica a vapore sul Lago di Ginevra e rientro nel pomeriggio.",
                new BigDecimal("640.00"),
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 23),
                10,
                "Losanna",
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1527668752968-14dc70a27c95?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Alessandro R.", 5, "16 Ott 2026", "Panorami mozzafiato sul Lago di Ginevra! I vigneti di Lavaux sono un posto unico al mondo."),
                        new SeedReview("Elena V.", 5, "08 Ott 2026", "Museo Olimpico bellissimo e crociera in battello storico super rilassante.")
                )
        );

        Trip losannaDegustazione = createTrip(
                "Escursione & Degustazione Vini a Lavaux con Crociera",
                "Attività giornaliera con partenza dal porto di Losanna-Ouchy. Navigazione sul Lago di Ginevra e degustazione guidata di Chasselas presso una cantina storica." +
                        "\n@@STEP@@Imbarco al Molo di Losanna-Ouchy||40 min||⛴️ Biglietto battello incluso||Partenza dal porto turistico di Ouchy e navigazione panoramica lungo la costa terrazzata di Lavaux." +
                        "\n@@STEP@@Passeggiata tra i Filari UNESCO di Saint-Saphorin||50 min||📍 Guida sommelier locale||Camminata agevole tra i muretti a secco medievali e spiegazione del metodo dei 'tre soli' che scalda le uve sul lago." +
                        "\n@@STEP@@Degustazione in Cantina Storica con Tagliere Alpino||1 ora 15 min||🍷 4 Vini + Tagliere inclusi||Assaggio guidato di 4 annate di Chasselas e Pinot Noir accompagnati da formaggi Gruyère DOP e pane di segale." +
                        "\n@@STEP@@Rientro in Treno Panoramico a Losanna||25 min||🚆 Biglietto rientro incluso||Ritorno diretto alla stazione centrale di Losanna costeggiando l'acqua.",
                new BigDecimal("85.00"),
                LocalDate.of(2026, 11, 21),
                LocalDate.of(2026, 11, 21),
                8,
                "Losanna",
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1527668752968-14dc70a27c95?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Federico M.", 5, "14 Ott 2026", "Vini Chasselas eccellenti e tagliere di Gruyère abbondante con vista sul lago."),
                        new SeedReview("Sara T.", 4, "03 Ott 2026", "Bellissima escursione pomeridiana, molto comodo il rientro in treno panoramico incluso.")
                )
        );

        Trip parigiViaggio = createTrip(
                "Parigi Bohémien: Montmartre, Louvre e Senna (5 Giorni)",
                "Scopri il fascino senza tempo della Ville Lumière: hotel 4 stelle nel quartiere Marais, ingresso riservato al Museo del Louvre, salita sulla Torre Eiffel e crociera sulla Senna." +
                        "\n@@STEP@@Giorno 1: Quartiere Le Marais & Place des Vosges||Giorno 1||🏨 Hotel 4* nel Marais incluso||Arrivo a Parigi, check-in e passeggiata guidata tra le gallerie d'arte e i cortili storici del Marais." +
                        "\n@@STEP@@Giorno 2: Museo del Louvre & Giardini delle Tuileries||Giorno 2||🎟️ Ingresso prioritario Louvre||Mattinata dedicata ai capolavori del Louvre con storico dell'arte; pomeriggio tra Place Vendôme e l'Opéra Garnier." +
                        "\n@@STEP@@Giorno 3: Torre Eiffel al Tramonto & Crociera sulla Senna||Giorno 3||⛴️ Salita Eiffel + Crociera incluse||Accesso al secondo livello della Torre Eiffel e navigazione serale lungo i ponti illuminati di Parigi." +
                        "\n@@STEP@@Giorni 4-5: Collina di Montmartre, Sacré-Cœur e Rientro||Giorni 4-5||📍 Tour artistico guidato||Esplorazione di Place du Tertre, Basilica del Sacro Cuore e tempo libero sugli Champs-Élysées prima del rientro.",
                new BigDecimal("720.00"),
                LocalDate.of(2026, 12, 4),
                LocalDate.of(2026, 12, 8),
                14,
                "Parigi",
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Valentina C.", 5, "19 Ott 2026", "Hotel nel Marais in posizione perfetta e crociera serale sulla Senna davvero magica."),
                        new SeedReview("Marco L.", 5, "11 Ott 2026", "Zero code al Louvre e alla Torre Eiffel grazie agli ingressi riservati."),
                        new SeedReview("Beatrice N.", 4, "02 Ott 2026", "Tour di Montmartre molto curato, guida simpaticissima.")
                )
        );

        Trip amalfiCostiera = createTrip(
                "Costiera Amalfitana, Positano & Capri in Barca (4 Giorni)",
                "Soggiorno panoramico ad Amalfi con escursione privata in gozzo sorrentino verso i Faraglioni di Capri, trekking sul Sentiero degli Dei e degustazione di limoncello." +
                        "\n@@STEP@@Giorno 1: Duomo di Amalfi & Limoneti Storici||Giorno 1||🍋 Degustazione in limoneto inclusa||Arrivo ad Amalfi, visita del Duomo arabo-normanno e passeggiata nei giardini terrazzati di limoni Sfusato Amalfitano." +
                        "\n@@STEP@@Giorno 2: Crociera in Gozzo Privato a Capri e Faraglioni||Giorno 2||⛵ Barca privata per l'intera giornata||Navigazione lungo la costa verso Capri, sosta bagno ai Faraglioni, Grotta Bianca e pomeriggio libero in Piazzetta." +
                        "\n@@STEP@@Giorno 3: Sentiero degli Dei & Borgo di Positano||Giorno 3||🥾 Guida escursionistica ufficiale||Trekking panoramico da Bomerano a Nocelle sospesi tra cielo e mare, con discesa finale tra le boutique di Positano." +
                        "\n@@STEP@@Giorno 4: Villa Rufolo a Ravello & Partenza||Giorno 4||🎟️ Ingresso giardini Villa Rufolo||Mattinata sulla terrazza dell'Infinito a Ravello e rientro.",
                new BigDecimal("580.00"),
                LocalDate.of(2026, 11, 26),
                LocalDate.of(2026, 11, 29),
                3,
                "Amalfi",
                agenziaItalia,
                List.of(
                        "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Mario Rossi", 5, "21 Ott 2026", "Il giro in gozzo privato a Capri e il Sentiero degli Dei valgono da soli l'intero viaggio!"),
                        new SeedReview("Sofia G.", 5, "13 Ott 2026", "Esperienza da 5 stelle piene, Ravello e Positano indimenticabili."),
                        new SeedReview("Andrea B.", 5, "05 Ott 2026", "Degustazione nei limoneti storici di Amalfi autentica e curatissima.")
                )
        );

        Trip tokyoAvventura = createTrip(
                "Tokyo Futuristica, Kyoto & Monte Fuji (9 Giorni)",
                "Un'immersione totale nella cultura giapponese: dai templi storici di Asakusa all'incrocio di Shibuya, escursione al Monte Fuji e viaggio in Shinkansen verso Kyoto." +
                        "\n@@STEP@@Giorni 1-3: Tokyo Storica (Asakusa) & Luci di Shinjuku||Giorni 1-3||🏨 Hotel a Tokyo + Welcome Dinner||Visita al tempio Senso-ji, santuario Meiji, incrocio di Shibuya e osservatorio panoramico di Shinjuku." +
                        "\n@@STEP@@Giorni 4-5: Lago Kawaguchiko & Vista Monte Fuji||Giorni 4-5||🗻 Escursione + Ryokan con Onsen||Partenza per la regione dei Cinque Laghi ai piedi del Monte Fuji, funivia panoramica e pernottamento in Ryokan tradizionale." +
                        "\n@@STEP@@Giorni 6-8: Treno Proiettile Shinkansen verso Kyoto & Arashiyama||Giorni 6-8||🚄 Pass Shinkansen incluso||Viaggio ad alta velocità verso Kyoto: Padiglione d'Oro, foresta di bambù di Arashiyama e santuario Fushimi Inari." +
                        "\n@@STEP@@Giorno 9: Rientro a Tokyo, Mercato di Tsukiji & Partenza||Giorno 9||🍣 Street food tour incluso||Ultime ore dedicate alla gastronomia giapponese e trasferimento in aeroporto.",
                new BigDecimal("1890.00"),
                LocalDate.of(2027, 1, 15),
                LocalDate.of(2027, 1, 24),
                9,
                "Tokyo",
                agenziaEuropa,
                List.of(
                        "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1000&q=80",
                        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=1000&q=80"
                ),
                List.of(
                        new SeedReview("Luca D.", 5, "17 Ott 2026", "Il pernottamento in Ryokan ai piedi del Monte Fuji e Kyoto sono stati un sogno."),
                        new SeedReview("Martina P.", 5, "10 Ott 2026", "Organizzazione perfetta negli spostamenti con lo Shinkansen, guida in italiano super disponibile.")
                )
        );

        // 3. CREAZIONE LISTE PREFERITI
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

        // 5. CREAZIONE NOTIFICHE REALI
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

        System.out.println("✅ DATABASE HORIZON POPOLATO CON RECENSIONI E VALUTAZIONI REALI SINCRONIZZATE!");
    }

    private Trip createTrip(
            String title,
            String descriptionWithSteps,
            BigDecimal price,
            LocalDate startDate,
            LocalDate endDate,
            int spots,
            String location,
            User organizer,
            List<String> images,
            List<SeedReview> realReviews
    ) {
        StringBuilder fullDesc = new StringBuilder(descriptionWithSteps);
        for (SeedReview rev : realReviews) {
            fullDesc.append("\n@@REVIEW@@")
                    .append(rev.author).append("||")
                    .append(rev.rating).append("||")
                    .append(rev.date).append("||")
                    .append(rev.comment);
        }

        int count = realReviews.size();
        double avg = realReviews.stream().mapToInt(r -> r.rating).average().orElse(0.0);
        double roundedAvg = Math.round(avg * 10.0) / 10.0;

        Trip t = new Trip();
        t.setTitle(title);
        t.setDescription(fullDesc.toString());
        t.setPrice(price);
        t.setStartDate(startDate);
        t.setEndDate(endDate);
        t.setAvailableSpots(spots);
        t.setLocation(location);
        t.setAverageRating(roundedAvg);
        t.setReviewCount(count);
        t.setOrganizer(organizer);
        t.setImageUrls(images);
        return tripRepository.save(t);
    }
}