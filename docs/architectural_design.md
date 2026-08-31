# Architettura del Software - NEPE 2.0

Questo documento descrive dettagliatamente la struttura dei pacchetti, la disposizione delle cartelle e i flussi di comunicazione scelti per lo sviluppo di **NEPE 2.0**. 

Il progetto adotta un approccio ibrido che unisce i vantaggi dell'**Architettura Esagonale (Ports & Adapters)** con la modularità dell'organizzazione **Feature-by-Package**, garantendo massima manutenibilità, isolamento dei calcoli matematici ed indipendenza dai framework tecnologici (JavaFX, Spring Boot, MariaDB).

---

## 1. Principi Guida dell'Architettura

### 1.1 Architettura Esagonale (Ports and Adapters)
Il principio cardine è la **regola di dipendenza verso l'interno**: il nucleo dell'applicazione (Domain) non deve dipendere da database, interfacce grafiche o framework esterni.
* **Domain Core (Il Nucleo):** Contiene la logica di business pura (il motore matematico di Poisson e Dixon-Coles, la logica di EV e i modificatori). È scritto in **Java puro** (senza annotazioni Spring o JPA).
* **Ports (Le Porte):** Interfacce che definiscono i contratti per interagire con il dominio.
  * *Inbound Ports (Driving):* Definiscono cosa l'applicazione può fare (es. `CalculatePreMatchEvUseCase`).
  * *Outbound Ports (Driven):* Definiscono di cosa ha bisogno il dominio dall'esterno (es. `MatchRepositoryPort` per salvare dati).
* **Adapters (Gli Adattatori):** Implementano o utilizzano le porte per connettersi a tecnologie esterne.
  * *Inbound Adapters:* La GUI in JavaFX (Controller) che chiama i casi d'uso.
  * *Outbound Adapters:* I DAO in Spring Data/JDBC che implementano le porte di persistenza su MariaDB, o il parser CSV.

### 1.2 Perché combinare Esagonale e Feature-by-Package?
Una delle critiche classiche all'Architettura Esagonale pura è la dispersione delle classi: se organizziamo per layer globali (`domain`, `port`, `adapter`), per aggiungere una feature dobbiamo lavorare su 5-6 pacchetti distanti.
Allo stesso modo, il *Feature-by-Package* puro rischia di mescolare dettagli DB e logica grafica se non ha regole di isolamento interne.

**Combinare i due approcci risolve entrambi i problemi:**
* La struttura a livello radice è organizzata per aree di business (es. `match`, `competition`, `inference`, `settings`), rendendo la mappa del progetto immediatamente leggibile.
* Ciascuna area di business è isolata al suo interno tramite l'esagono (`domain`, `port`, `adapter`), garantendo che il codice grafico (JavaFX) o il database (JPA/Hibernate) non sporchino le regole matematiche o i calcoli.

**Vantaggi:**
* **Alta Coesione:** Tutto il codice relativo a una specifica feature (es. la gestione dei Match) è raggruppato nello stesso posto.
* **Basso Accoppiamento:** Modificare una funzionalità non impatta le altre.
* **Facilità di Navigazione:** Lo sviluppatore trova immediatamente tutto ciò che serve per una feature senza saltare tra cartelle distanti.

---

## 2. Struttura dei Pacchetti (Folder Tree)

Di seguito viene illustrato l'albero delle cartelle del progetto all'interno del sorgente Java (`src/main/java`):

com.nexus.nepe -> org.nepe
```text
org.nepe
│
├── shared                         # Elementi comuni riutilizzabili da più feature
│   └── exception
│       ├── NepeException.java
│       ├── DomainValidationException.java
│       ├── EntityNotFoundException.java
│       ├── DataImportException.java
│       ├── AliasMappingRequiredException.java
│       ├── LiveTradingException.java
│       └── GuiException.java
│
├── competition                    # FEATURE: Gestione Campionati, Squadre e Alias
│   ├── domain/                    # Competition.java, Season.java, Team.java, TeamAlias.java
│   ├── port/in/                   # ManageCompetitionUseCase.java, ManageSeasonUseCase.java, ManageTeamUseCase.java
│   ├── port/out/                  # CompetitionRepositoryPort.java, SeasonRepositoryPort.java, TeamRepositoryPort.java, TeamAliasRepositoryPort.java
│   ├── service/                   # CompetitionService.java, SeasonService.java, TeamService.java
│   └── adapter/
│       ├── in/                    # CompetitionViewController.java, AliasMappingController.java (JavaFX)
│       └── out/                   # Repositories, JPA Entities, Mappers
│
├── match                          # FEATURE: Palinsesto, CRUD, Eventi, Quote e Ingestione CSV
│   ├── domain/                    # Match.java, MatchEvent.java, MatchStatistics.java, MatchModifiers.java, MarketOdds.java
│   ├── port/in/                   # ManageMatchUseCase.java, LiveMatchTradingUseCase.java, ManageMarketOddsUseCase.java, ImportCsvMatchesUseCase.java
│   ├── port/out/                  # MatchRepositoryPort.java, MatchEventRepositoryPort.java, MarketOddsRepositoryPort.java, MatchDetailsRepositoryPort.java, CsvParserPort.java
│   ├── service/                   # MatchService.java, LiveMatchTradingService.java, MarketOddsService.java, ImportCsvMatchesService.java
│   └── adapter/
│       ├── in/                    # DashboardController.java, LiveConsoleController.java (JavaFX)
│       └── out/                   # CsvParserAdapter.java, Repositories, JPA Entities, Mappers
│
├── inference                      # FEATURE: Motore Matematico di Calcolo (Inference Engine)
│   ├── domain/                    # PoissonModel.java, DixonColesModel.java, EvCalculator.java, TeamStrengthCalculator.java, XgEstimator.java, LiveEngineModifiers.java
│   ├── port/in/                   # CalculatePreMatchInferenceUseCase.java, CalculateLiveInferenceUseCase.java
│   ├── service/                   # PreMatchInferenceService.java, LiveInferenceService.java
│   └── adapter/in/                # PreMatchAnalysisController.java (JavaFX)
│
├── settings                       # FEATURE: Preferenze e Parametri Globali
│   ├── domain/                    # AppSettings.java
│   ├── port/in/ & port/out/       # ManageSettingsUseCase.java, SettingsRepositoryPort.java
│   ├── service/                   # SettingsService.java
│   └── adapter/
│       ├── in/                    # SettingsViewController.java (JavaFX)
│       └── out/                   # SettingsRepositoryAdapter.java, JPA Entity, Mapper
│
└── bootstrap                      # Configurazione infrastrutturale e colla Spring/JavaFX
    ├── NepeApplication.java       # Main Spring Boot Application
    ├── JavaFxApplication.java     # Bootstrap del ciclo di vita JavaFX
    └── SpringFXMLLoader.java      # FXML Loader personalizzato integrato con Spring
```

---

## 3. Flusso dei Dati ed Inversione delle Dipendenze

Per rispettare la separazione dei ruoli, le dipendenze puntano sempre verso l'interno. Di seguito è mostrato il flusso logico di una richiesta di calcolo e salvataggio:

```mermaid
graph TD
    subgraph Adattatore Inbound (GUI)
        A[JavaFX View / Controller]
    end

    subgraph Porta Inbound (Port Driving)
        B[CalculateEvUseCase - Interface]
    end

    subgraph Core di Dominio (Business Logic)
        C[EvCalculator - Domain Logic]
        D[Match - Domain Entity]
    end

    subgraph Porta Outbound (Port Driven)
        E[MatchRepositoryPort - Interface]
    end

    subgraph Adattatore Outbound (Persistenza)
        F[MatchRepositoryAdapter - SQL Implementation]
        G[Spring Data JPA / JDBC]
        H[(MariaDB Database)]
    end

    A -->|1. Invoca tramite DTO| B
    B -->|2. Esegue logica| C
    C -->|3. Manipola entità| D
    C -->|4. Salva tramite| E
    F -->|5. Implementa porta| E
    F -->|6. Chiama| G
    G -->|7. Persiste| H

    style C fill:#f9f,stroke:#333,stroke-width:2px
    style D fill:#f9f,stroke:#333,stroke-width:2px
```

### 3.1 Spiegazione del Flusso
1. **JavaFX Controller (Adapter Inbound)** raccoglie l'input dell'utente e lo incapsula in un DTO. Chiama l'interfaccia **`CalculateEvUseCase` (Porta Inbound)**.
2. Il servizio applicativo all'interno del dominio implementa la porta inbound. Esso richiama gli algoritmi matematici nel **Domain Core** (`EvCalculator`, `PoissonModel`) per calcolare l'Expected Value.
3. Il dominio aggiorna lo stato dell'entità di dominio pura **`Match`** (es. aggiorna le quote stimate).
4. Il dominio ha bisogno di salvare i dati. Per farlo, chiama l'interfaccia **`MatchRepositoryPort` (Porta Outbound)**.
5. L'adattatore infrastrutturale **`MatchRepositoryAdapter` (Adapter Outbound)** implementa l'interfaccia della porta outbound. Esso mappa l'entità di dominio pura `Match` sull'entità JPA/SQL ed esegue la persistenza tramite **Spring Data / JDBC** su **MariaDB**.

---

## 4. Disaccoppiamento della Persistenza (Domain vs JPA vs Mapper)

Seguendo l'approccio della **Purezza Esagonale (Opzione A)**, lo strato di persistenza database è completamente disaccoppiato dal dominio. Le responsabilità sono ripartite nel seguente modo all'interno di ciascuna feature (es. `match`):

1. **Domain Entity (`Match`):** È una classe Java pura (POJO / Record). Contiene solo variabili di stato, costruttori e metodi di business (es. calcolo xG dinamico, applicazione modificatori). Non contiene annotazioni come `@Entity`, `@Table` o `@Column`.
2. **JPA Entity (`MatchJpaEntity`):** È una classe situata all'interno del pacchetto `adapter.out`. È annotata con `@Entity`, `@Table` ed esprime il mapping relazionale con MariaDB (usando `@Column`, `@Id`, ecc.). Questa classe è di esclusivo utilizzo dell'adapter e non viene mai esposta al dominio o alla GUI.
3. **Spring Data Repository (`SpringDataMatchRepository`):** Interfaccia Java che estende `JpaRepository<MatchJpaEntity, Integer>`. Viene implementata automaticamente da Spring Boot per gestire le operazioni CRUD standard su database tramite Hibernate.
4. **Mapper (`MatchMapper`):** Componente dell'adattatore che si occupa di convertire le classi:
   * `Match` (Domain) $\to$ `MatchJpaEntity` (JPA) in fase di salvataggio/scrittura.
   * `MatchJpaEntity` (JPA) $\to$ `Match` (Domain) in fase di caricamento/lettura dal DB.

### 4.1 Esempio Logico di Integrazione
Nel codice del nostro Outbound Adapter (`MatchRepositoryAdapter`):
```java
@Repository
public class MatchRepositoryAdapter implements MatchRepositoryPort {

    private final SpringDataMatchRepository springDataRepository;
    private final MatchMapper mapper;

    @Override
    public Match save(Match match) {
        // 1. Mappa l'entità di dominio pura in un'entità database
        MatchJpaEntity jpaEntity = mapper.toJpa(match);
        
        // 2. Salva l'entità database tramite Spring Data JPA
        MatchJpaEntity savedEntity = springDataRepository.save(jpaEntity);
        
        // 3. Riconverte l'entità salvata in entità di dominio per il ritorno
        return mapper.toDomain(savedEntity);
    }
}
```

> [!NOTE]
> Questo isolamento ci permette in futuro di cambiare interamente la tecnologia di persistenza (es. passare da JPA a query SQL native tramite `JdbcTemplate` o `JdbcClient`) modificando **esclusivamente** il codice dentro il pacchetto `adapter.out`. Il Domain Core rimarrà intatto e non subirà alcuna variazione o ricompilazione.

---

## 5. Dettaglio sull'Integrazione Spring Boot + JavaFX

Una delle sfide principali nello sviluppo di desktop app Java moderne è coniugare il ciclo di vita di **JavaFX** (gestito dal thread di applicazione JavaFX e caricato tramite file `.fxml`) con l'Inversion of Control (IoC) di **Spring Boot**.

### 5.1 Soluzione Architetturale (SpringFXMLLoader)
Per permettere l'iniezione delle dipendenze di Spring nei controller delle schermate JavaFX (annotati con `@FXML`), implementiamo un `SpringFXMLLoader`:
1. All'avvio dell'applicazione, Spring Boot viene inizializzato.
2. JavaFX viene avviato, ma anziché usare il `FXMLLoader` standard di Java, utilizziamo un `SpringFXMLLoader` personalizzato a cui passiamo l'**`ApplicationContext` di Spring**.
3. Il caricatore personalizzato sovrascrive la factory dei controller:
   ```java
   fxmlLoader.setControllerFactory(applicationContext::getBean);
   ```
4. Questo garantisce che ogni volta che JavaFX carica un file FXML, il controller corrispondente venga istanziato come un **Bean di Spring**, consentendo l'uso di `@Autowired` o dell'iniezione tramite costruttore per accedere ai nostri `UseCase` del dominio.

---

## 6. DAO Verticali e DTO

* **DAO Verticali (Data Access Objects):** Ogni adapter di persistenza gestisce le operazioni di lettura e scrittura della propria feature (es. `MatchRepositoryAdapter` interroga MariaDB solo per i dati relativi ai match). I repository Spring Data sottostanti non vengono mai esposti al di fuori del pacchetto `adapter.out` corrispondente.
* **DTO (Data Transfer Objects):** I DTO vengono utilizzati per far transitare i dati attraverso la barriera dell'esagono. Questo impedisce che modifiche alle entità di dominio (es. aggiunta di metodi matematici in `Match.java`) impattino direttamente la GUI JavaFX o che le modifiche grafiche costringano a alterare la logica del dominio.

---

## 7. Gestione delle Eccezioni Personalizzate (Unchecked Exceptions)

Per garantire la massima robustezza agli errori senza appesantire la firma dei metodi con dichiarazioni `throws` (tipiche delle checked exceptions), NEPE 2.0 implementa una **rete di eccezioni personalizzate non controllate (Unchecked Exceptions)** che estendono `RuntimeException`.

### 7.1 Gerarchia delle Eccezioni
Tutte le eccezioni ereditano da una classe base comune inserita nel pacchetto `shared.domain`:
* **`NepeException` (extends `RuntimeException`):** Classe base globale per le eccezioni dell'applicazione.
  * **`EntityNotFoundException`:** Lanciata quando un'entità richiesta (squadra, competizione, match, alias) non è presente nel DB.
  * **`DomainValidationException`:** Lanciata quando una regola di business o una validazione matematica viene violata (es. quote negative, punteggio inferiore a zero, minutaggio live non valido).
  * **`DataImportException`:** Lanciata durante il parsing del CSV per colonne mancanti, formato data non riconosciuto o righe corrotte.
  * **`AliasMappingRequiredException`:** Lanciata durante l'importazione se un nome squadra non viene riconosciuto nel database. Questa eccezione viene catturata dal controller JavaFX per bloccare l'importazione e mostrare a schermo il popup di mappatura alias.
  * **`LiveTradingException`:** Lanciata per operazioni non permesse sulla console live (es. inserire un evento su un match già concluso o annullato).

### 7.2 Traduzione delle Eccezioni (Exception Translation Pattern)
Negli adattatori outbound (DB), le eccezioni SQL native (come violazioni di chiavi esterne o unicità di MariaDB) non vengono mai propagate verso il dominio. L'adapter cattura l'eccezione JDBC/JPA e la traduce in un'eccezione di dominio coerente:
```java
try {
    springDataRepository.save(entity);
} catch (DataIntegrityViolationException e) {
    throw new DomainValidationException("Impossibile salvare: violazione dei vincoli sui dati storici.", e);
}
```

---

## 8. Definizione delle Viste JavaFX (GUI) e Controller

La struttura dei controller JavaFX riflette le aree operative dell'applicazione. Avremo **6 viste principali** (ciascuna associata a un file `.fxml` e al relativo controller registrato come Spring Bean nel rispettivo pacchetto `adapter.in`):

| Vista FXML | Controller JavaFX | Descrizione |
| :--- | :--- | :--- |
| `dashboard.fxml` | `DashboardController` | **Schermata Principale:** Mostra il palinsesto delle partite della competizione attiva, filtri stagioni, pulsanti per importazione CSV e navigazione verso le altre aree. |
| `competition_manager.fxml` | `CompetitionViewController` | **Gestione Anagrafiche:** CRUD per competizioni, squadre e alias. |
| `pre_match_analysis.fxml` | `PreMatchAnalysisController` | **Analisi Pre-Match:** Visualizzazione delle quote stimate (Dixon-Coles + Poisson), inserimento quote Back/Lay dell'Exchange, calcolo EV (classico e tarato sul rischio), e configurazione dei modificatori. |
| `live_console.fxml` | `LiveConsoleController` | **Console Live:** Console di inserimento eventi in tempo reale (gol, rossi), aggiornamento del minuto, ricalcolo istantaneo delle probabilità residue e allarmi per Green Up. |
| `alias_mapping_popup.fxml` | `AliasMappingController` | **Popup Modale di Mapping:** Finestra di dialogo bloccante che compare durante il caricamento CSV se viene rilevata una squadra sconosciuta. |
| `settings.fxml` | `SettingsViewController` | **Pannello Impostazioni:** Modifica persistente dei parametri globali (commissioni, decay $\gamma$, campione $N$). |
