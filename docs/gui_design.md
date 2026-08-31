# Specifiche dell'Interfaccia Utente e Flussi di Navigazione - NEPE

Questo documento descrive in dettaglio il design dell'esperienza utente (UX), il layout visivo e il comportamento funzionale di ciascuna schermata della GUI JavaFX di **NEPE**.

Tutte le schermate seguono una filosofia di design **moderna, reattiva e orientata ai dati** per consentire al trader sportivo di prendere decisioni oggettive nel minor tempo possibile.

---

## 1. Struttura Generale del Layout (Shell Principale)

L'applicazione adotta una struttura a **Dashboard con Navigazione Laterale (Sidebar)**:
* **Sidebar Sinistra (Fissa):** Contiene il logo NEPE, l'indicatore della **Competizione Selezionata** attiva ed i pulsanti di navigazione principale (Dashboard, Gestione Anagrafiche, Impostazioni).
* **Header Superiore (Fisso):** Mostra la stagione attiva (es. `2025/2026`), lo stato della connessione al DB locale MariaDB, il pulsante rapido **"Importa CSV"** e l'orologio locale sincronizzato con l'orario UTC salvato nel DB.
* **Area di Contenuto Centrale (Dinamica):** Area che ospita le varie schermate `.fxml` scambiate dinamicamente durante la navigazione.

---

## 2. Dettaglio delle Schermate

### 2.1 Dashboard (`dashboard.fxml` / `DashboardController`)
È la schermata principale che si apre all'avvio dell'applicazione.

* **Scopo:** Fornire una panoramica immediata del palinsesto delle partite della competizione attualmente selezionata dall'utente.
* **Componenti Visivi principali:**
  * **Barra di Selezione Competizione:** Menu a tendina per cambiare al volo la competizione attiva (es. *Serie A*, *Premier League*). L'intera GUI aggiorna contestualmente i dati mostrati.
  * **Filtro Stato Match:** Schede/Tab per filtrare le partite tra *Programmate (SCHEDULED)*, *In Corso (LIVE)*, *Terminate (FINISHED)* e *Tutte*.
  * **Tabella Palinsesto (TableView JavaFX):**
    * Colonne: *Data/Ora (convertita in CET/CEST)*, *Squadra Casa*, *Squadra Ospite*, *Stato*, *Risultato (se giocata)*, *Quota 1X2 Riferimento*, *Indicatori EV (Badge colorati per opportunità)*.
    * **Origine della Quota di Riferimento:** La quota 1X2 mostrata nel palinsesto proviene dalle colonne medie di mercato del CSV (`AvgH`, `AvgD`, `AvgA` o fallback `B365H`/`D`/`A`) memorizzate in `odds_home`, `odds_draw`, `odds_away` della tabella `matches` (o inserite a mano per match creati da GUI).
    * **Calcolo dell'EV Sintetico nella Dashboard:** Per fornire un segnale visivo immediato senza dover aprire ogni singola partita, l'Inference Engine calcola automaticamente in background l'EV 1X2 confrontando le probabilità stimate (Poisson + Dixon-Coles) con questa quota di riferimento. Se uno degli esiti (1, X, 2) ha un $\text{EV} > 0$, nella riga compare un badge visivo (es. badge verde `EV+ 1 (7.5%)`), invitando l'utente ad approfondire l'analisi nella schermata pre-match.
    * Azioni per riga: Pulsante **"Analizza Pre-Match"** e Pulsante **"Apri Console Live"** (disponibile se il match è LIVE).
  * **Barra di Azione In basso:** Pulsante principale *"Importa Nuova Giornata (CSV)"* e pulsante *"Aggiungi Partita Manualmente"*.

---

### 2.2 Gestione Anagrafiche e Competizioni (`competition_manager.fxml` / `CompetitionViewController`)
Schermata dedicata al controllo ed all'operatività manuale totale sulle entità di base.

* **Scopo:** Permettere la gestione CRUD (Creazione, Lettura, Modifica, Eliminazione) per competizioni, squadre e alias.
* **Componenti Visivi principali:**
  * **Sezione Competizioni:** Tabella con l'elenco dei campionati (`code`, `name`, `country`, `dixon_coles_rho`). Form laterale per modificare il coefficiente $\rho$ o aggiungere nuovi campionati.
  * **Sezione Squadre:** Elenco ricercabile di tutte le squadre memorizzate. Permette la rinomina di una squadra o la fusione/eliminazione.
  * **Sezione Gestione Alias (Team Aliases):** Tabella delle associazione tra i nomi grezzi usati nei CSV/Bookmaker e i nomi ufficiali della squadra nel DB. Permette di aggiungere a mano un alias mancante o correggere un'associazione errata.

---

### 2.3 Analisi Pre-Match e Calcolo dell'EV (`pre_match_analysis.fxml` / `PreMatchAnalysisController`)
È il cuore analitico del programma per la fase prima dell'inizio delle partite.

* **Scopo:** Presentare le probabilità reali calcolate dal motore matematico e confrontarle con le quote di mercato inserite dall'utente per evidenziare le scommesse a Valore Atteso Positivo ($\text{EV} > 0$).
* **Ordinamento e Flusso di Lavoro:**
  * L'utente seleziona una partita dal palinsesto della Dashboard ed accede a questa vista.
  * **Fase 1 - Probabilità Pure:** Il sistema calcola immediatamente le percentuali di accadimento per 1X2, Under/Over 0.5-4.5 e BTTS basandosi su Poisson + Dixon-Coles e le mostra a schermo (con relative "quote eque" teoriche).
  * **Fase 2 - Inserimento Quote Exchange:** L'utente inserisce manualmente le quote correnti di mercato Punta (Back) e/o Banca (Lay) ricavate dai siti di exchange (es. Betfair).
  * **Fase 3 - Calcolo Immediato dell'EV:** Non appena l'utente digita una quota nel campo di testo (o usa le frecce su/giù), l'Inference Engine ricalcola istantaneamente:
    * $\text{EV}_{\text{Punta, Comm}}$ (Valore atteso netto per la posizione Punta).
    * $\text{EV}_{\text{Banca, Comm}}$ (Valore atteso netto per la posizione Banca).
    * $\text{EV}_{\text{Banca, Rischio}}$ (Valore atteso rapportato alla responsabilità/capitale a rischio).
  * **Evidenziazione Visiva:** Se un EV supera lo 0%, il box corrispondente si illumina di verde con l'indicazione della percentuale di vantaggio.
* **Sezione Modificatori di Partita (Accordion/Pannello Espandibile):**
  * Slider per i moltiplicatori di attacco/difesa manuali (infortuni, formazioni rimaneggiate).
  * Checkbox per `must_win_home`, `must_win_away`, `low_urgency_home`, `low_urgency_away`, `is_neutral_venue`.
  * Ogni modifica ai checkbox o slider aggiorna in tempo reale la matrice dei punteggi e tutti gli EV calcolati.

---

### 2.4 Console Live (`live_console.fxml` / `LiveConsoleController`)
Interfaccia di controllo in tempo reale durante lo svolgimento degli incontri.

* **Scopo:** Fornire uno strumento tattico per monitorare l'evoluzione delle probabilità mentre il tempo scorre e registrare gol/espulsioni.
* **Componenti Visivi principali:**
  * **Header del Match:** Nomi delle squadre, punteggio corrente a grandi cifre, e **Minuto Corrente (campo numerico editabile o pulsanti +1m, +5m)**.
  * **Pulsanti di Azione Rapida Eventi:**
    * Pulsante verde *"Gol Casa"* / *"Gol Ospite"*
    * Pulsante rosso *"Cartellino Rosso Casa"* / *"Cartellino Rosso Ospite"*
    * Pulsante *"Annulla Ultimo Evento"*
  * **Pannello Probabilità Residue (Real-Time Grid):** Grafici visivi e tabelle con le probabilità aggiornate per i gol rimanenti nel tempo residuo (tenendo conto di Time-Decay, Rossis e Must-Win nel 2° tempo).
  * **Target di Uscita (Green Up / Cash Out):** Cella visiva che segnala quando le probabilità residue hanno raggiunto le soglie di profitto impostate per suggerire la chiusura della posizione in guadagno.

---

### 2.5 Popup Modale Mappatura Alias (`alias_mapping_popup.fxml` / `AliasMappingController`)
Finestra di dialogo modale che compare in caso di rilevamento di una squadra non ancora censita o mappata durante l'importazione CSV.

* **Scopo:** Garantire l'integrità del database evitando che nomi con spelling diverso creino squadre duplicate (*Human-in-the-Loop*).
* **Comportamento UX:**
  * Mostra il messaggio: *"Squadra non riconosciuta nel file CSV: '[Nome CSV]'"*.
  * Offre due opzioni:
    1. **Associa a squadra esistente:** Menu a tendina con ricerca per scegliere la squadra reale dal DB. Cliccando "Conferma Associazione", il sistema crea il record in `team_aliases` e riprende automaticamente e in modo trasparente l'elaborazione del file CSV dal punto di interruzione.
    2. **Crea Nuova Squadra:** Salva la squadra come nuova entità nel DB, associa l'alias e prosegue l'importazione.
    3. **Annulla:** Interrompe l'importazione del file corrente e notifica l'annullamento.

---

### 2.6 Pannello Impostazioni (`settings.fxml` / `SettingsViewController`)
Schermata di configurazione dei parametri globali del software.

* **Scopo:** Consentire all'utente di personalizzare le costanti dei modelli matematici e di trading.
* **Campi Configurabili:**
  * *Tasso Commissione Exchange (%)*: Default 5% (utilizzato per depurare gli EV).
  * *Numero Partite Campione ($N$)*: Default 10 (numero di match storici per il calcolo delle forze $\alpha$ e $\beta$).
  * *Fattore Decadimento Stagionale ($\gamma$)*: Default 0.70 (peso attribuito alle partite della stagione precedente).
  * *Soglia Target Green Up (%)*: Default 10% (soglia per i suggerimenti di Cash Out live).
