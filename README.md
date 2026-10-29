# Horizon Travel — Piattaforma per la Pianificazione di Viaggi e Attività

Progetto universitario per la gestione, prenotazione e condivisione di pacchetti di viaggio multi-giorno ed escursioni giornaliere, con distinzione dei ruoli tra **Viaggiatori** e **Agenzie Organizzatrici (Partner)**.

## 1. Architettura del Sistema e Tecnologie Utilizzate

Il sistema è progettato secondo un'architettura Client-Server containerizzata:
* **Backend REST API**: Java 21, Spring Boot 3, Spring Security (Autenticazione Stateless con **JWT** e crittografia **BCrypt**), Spring Data JPA / Hibernate, ModelMapper, OpenAPI / Swagger UI.
* **Database Relazionale**: **PostgreSQL 15** (`horizon_postgres`) con persistenza su volume Docker e popolamento automatico iniziale (`DataInitializer.java`).
* **Servizio SMTP & Webmail di Test**: **Mailpit** (`horizon_mailpit`) per il test completo del flusso di recupero password tramite link via email (`http://localhost:8025`).
* **Applicazione Mobile Android**: Kotlin, **Jetpack Compose (Material Design 3)**, architettura **MVVM** (`ViewModel` + `State`), **Retrofit2 & OkHttp3** per la comunicazione REST, **Coil** per il caricamento asincrono delle immagini.

## 2. Funzionalità Principali Implementate

### Lato Viaggiatore (`VIAGGIATORE`)
* **Esplorazione e Ricerca Avanzata**: Catalogo suddiviso tra *Viaggi Organizzati Multi-Giorno* e *Attività / Escursioni in Giornata*, guide delle destinazioni iconiche, filtro per categorie tematiche, budget massimo, numero di partecipanti e ordinamento per valutazione o prezzo.
* **Dettaglio Pacchetto, Logistica e Prezzi Dinamici**:
  * Per i **Viaggi Multi-Giorno**: visualizzazione della scheda *Servizi di Viaggio e Ospitalità* (Mezzo di Trasporto, Struttura Alberghiera, Formula di Trattamento), finestra temporale ufficiale del pacchetto e programma giorno per giorno.
  * Per le **Attività Giornaliere**: calendario mensile interattivo con calcolo automatico delle tariffe stagionali/weekend e blocco automatico delle date passate o con posti insufficienti.
* **Checkout Mock e Ricevute con Nominativi**: Simulazione del pagamento (*Carta Visa/Mastercard*, *PayPal*, *Google Pay*) con inserimento obbligatorio dei **nominativi di tutti i partecipanti** (quando si prenota per più persone) e generazione della ricevuta digitale.
* **Esportazione in Formato Calendario (`.ics` & App Calendario)**: Possibilità di esportare qualsiasi viaggio o attività prenotata nello standard **RFC 5545 iCalendar (`.ics`)** o di inserirla direttamente nell'**App Calendario di sistema** di Android tramite `CalendarContract`.
* **Liste di Itinerari (Private, Condivise e Pubbliche)**: Creazione e gestione di liste personali, condivisione con amici registrati tramite email (con notifica in tempo reale) e generazione di *Capability Link* per le liste pubbliche.
* **Recensioni Verificate e Live Chat**: Pubblicazione di recensioni con ricalcolo automatico della media su PostgreSQL e **Live Chat** diretta con l'agenzia organizzativa 

### Lato Agenzia Organizzatrice (`ORGANIZZATORE`)
* **Console Partner & KPI**: Monitoraggio in tempo reale di ospiti attesi, ricevute emesse, incassi totali e *Rating Partner* calcolato sulle recensioni reali del catalogo.
* **Creazione e Modifica Pacchetti**: Pubblicazione di nuovi Viaggi Multi-Giorno o Attività Giornaliere, configurazione di Trasporto/Hotel/Trattamento, costruttore dinamico delle tappe (`Step-by-Step`) e gestione in tempo reale dei posti disponibili per singola data.
* **Registro Ricevute Ordini**: Consultazione ed esportazione delle ricevute di prenotazione complete di codice operazione, metodo di pagamento e lista numerata di tutti i partecipanti registrati.
* **Messaggistica Unificata con i Clienti**

## 3. Istruzioni di Avvio Rapido (Backend + Database + Mail Server)

### Prerequisiti
* **Docker Desktop** installato e avviato.
* **Android Studio** (con emulatore Android API 34+ configurato).

### Avvio del Backend tramite Docker Compose
Aprire un terminale nella cartella `backend` ed eseguire:

```bash
docker compose down -v
docker compose up -d --build

Avvio dell'Applicazione Android
1.	Aprire Android Studio e selezionare Open sulla cartella del progetto Android (frontend).
2.	Attendere il completamento del Gradle Sync.
3.	Avviare l'emulatore Android e premere Run 'app' (l'emulatore punta automaticamente a http://10.0.2.2:8080)

##4. Credenziali Pre-Configurate per il Test
1.	VIAGGIATORE – mario@horizon.it – Horizon2026!
2.	VIAGGIATORE – giulia@horizon.it – Horizon2026!
3.	ORGANIZZATORE – agenzia@horizon.it – Horizon2026!
4.	ORGANIZZATORE – alpi.tours@horizon.it – Horizon2026!

Vincenzo Calabro’, Matricola: 223527
