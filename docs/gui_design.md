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
  * **Header del Match & Stepper Minuti:** Nomi delle squadre, punteggio a grandi cifre, e **Minuto Corrente (campo numerico editabile e pulsanti rapidi -1m, +1m, +5m)**.
  * **Pulsanti di Azione Rapida Eventi:**
    * Pulsante verde *"Gol Casa"* / *"Gol Ospite"*
    * Pulsante rosso *"Cartellino Rosso Casa"* / *"Cartellino Rosso Ospite"*
    * Pulsante *"Annulla Ultimo Evento"* per ripristino istantaneo dello stato precedente.
  * **Griglia Quote Rapide Exchange e Valutazione Live EV (In-Memory / Volatili):**
    * Card operative disposte in layout a griglia simmetrica 50/50 per esiti 1X2 e Under/Over dinamico (con selettore soglia da 0.5 a 4.5).
    * Micro-etichette semantiche superiori ad alta leggibilità: `PUNTA (BACK)` e `BANCA (LAY)`.
    * Calcolo reattivo istantaneo dell'Expected Value (EV Punta ed EV Banca depurati da commissione exchange) e applicazione automatica del badge visuale verde per le quote a valore positivo ($\text{EV} > 0$).
    * Pulsante *"Azzera Quote Live Inserite"* per pulizia immediata della memoria volatile.
  * **Pannello Monitoraggio Posizione e Green-Up / Cash Out Reattivo:**
    * Configurazione della posizione d'ingresso con selezione direzione (*PUNTA (Long)* o *BANCA (Short)*), esito scommesso e quota d'apertura.
    * Stima in tempo reale della percentuale di profitto maturata rispetto al target configurato (`green_up_target`).
    * Banner di allerta ad alto contrasto visivo che si attiva automaticamente al raggiungimento del target di profitto, suggerendo l'uscita a mercato per bloccare il guadagno.
  * **Quadro Generale Probabilità e Quote Eque (TitledPane Collassabile):**
    * Sezione informativa completa con tutte le 5 soglie Under/Over (0.5, 1.5, 2.5, 3.5, 4.5) e BTTS Sì/No, racchiusa in un componente collassabile (`TitledPane` con default compresso `expanded="false"`) per ottimizzare lo spazio verticale e prevenire il sovraccarico cognitivo durante il trading live.

---

### 2.5 Popup Modali Operativi Complessi
Finestre di dialogo interattive dedicate a operazioni complesse di data-entry, configurazione e override manuale (*Human-in-the-Loop*). Per garantire continuità estetica con le schermate primarie, questi popup adottano integralmente il tema scuro dell'applicazione (`styles.css`).

#### 2.5.1 Popup Modale Mappatura Alias (`alias_mapping_popup.fxml` / `AliasMappingController`)
Finestra modale che compare in caso di rilevamento di una squadra non ancora censita o mappata durante l'importazione CSV.
* **Scopo:** Garantire l'integrità del database evitando che nomi con spelling diverso creino squadre duplicate.
* **Comportamento UX:**
  * Mostra il messaggio esplicativo: *"Squadra non riconosciuta nel file CSV: '[Nome CSV]'"*.
  * Offre tre opzioni operative:
    1. **Associa a squadra esistente:** Menu a tendina con ricerca dinamica per selezionare la squadra ufficiale dal DB. Pre-filtra di default le squadre della competizione corrente per massimizzare la rapidità operativa, con opzione di sblocco globale ("Mostra tutte le squadre"). Cliccando *"Conferma Associazione"*, il sistema crea il record in `team_aliases` e riprende automaticamente e in modo trasparente l'elaborazione del file CSV dal punto di interruzione.
    2. **Crea Nuova Squadra:** Salva la squadra come nuova entità nel DB, associa l'alias e prosegue l'importazione.
    3. **Annulla:** Interrompe l'importazione del file corrente e notifica l'annullamento.

#### 2.5.2 Inserimento Partita Manuale (`create_match_popup.fxml` / `CreateMatchController`)
Finestra modale richiamata dal pulsante *"Aggiungi Partita Manualmente"* della Dashboard.
* **Scopo:** Permettere al trader di pianificare e registrare incontri futuri (`SCHEDULED`) non presenti nei CSV storici (i quali contengono solo partite già disputate e concluse).
* **Componenti Visivi:**
  * Selezione guidata di squadra di casa e squadra ospite con vincolo di non coincidenza.
  * Selettore data (`DatePicker`) e orario di inizio in ora locale (automaticamente convertito e memorizzato in UTC nel DB).
  * Campi opzionali per quote 1X2 di riferimento iniziali (`odds_home`, `odds_draw`, `odds_away`).

#### 2.5.3 Modifica Statistiche e Override xG (`edit_match_stats_popup.fxml` / `EditMatchStatsController`)
Finestra modale per l'override dei dati di un match registrato nel DB.
* **Scopo:** Consentire l'intervento manuale su punteggio, tiri totali, tiri in porta, cartellini rossi e override esplicito degli xG (`manual_home_xg`, `manual_away_xg`).
* **Invarianti di Protezione:** Il salvataggio imposta automaticamente il flag `is_manually_edited = true`, impedendo che successivi re-import di file CSV sovrascrivano i dati validati manualmente dal trader.

---

### 2.6 Pannello Impostazioni (`settings.fxml` / `SettingsViewController`)
Schermata di configurazione dei parametri globali del software.

* **Scopo:** Consentire all'utente di personalizzare le costanti dei modelli matematici e di trading.
* **Campi Configurabili:**
  * *Tasso Commissione Exchange (%)*: Default 5% (utilizzato per depurare gli EV).
  * *Numero Partite Campione ($N$)*: Default 10 (numero di match storici per il calcolo delle forze $\alpha$ e $\beta$).
  * *Fattore Decadimento Stagionale ($\gamma$)*: Default 0.70 (peso attribuito alle partite della stagione precedente).
  * *Soglia Target Green Up (%)*: Default 10% (soglia per i suggerimenti di Cash Out live).

---

## 3. Design System, Finestre Modali e Contrasto Visivo (UX Guidelines)

NEPE applica una precisa gerarchia visiva basata sul contrasto cromatico per distinguere l'attività analitica continuativa dagli eventi di interruzione decisionale ad alta priorità.

### 3.1 Palette Cromatica Principale (Dark Theme Ergonomico)
* **Sfondo Primario:** `#121316` (riduce l'affaticamento visivo durante sessioni prolungate di trading e studio dei dati).
* **Container e Card:** `#16171b` / `#1a1b20` con bordi a basso contrasto (`#272a34`).
* **Tipografia:** San-serif di sistema moderna (`-apple-system`, `Segoe UI`, `Roboto`), testo primario ad alta leggibilità (`#f3f4f6`) e secondario smorzato (`#9ca3af`).
* **Segnali Semantici:**
  * Verde EV+ (`#22c55e` / `#16a34a`): Evidenzia quote a valore atteso positivo e indicatori di profitto.
  * Rosso (`#ef4444`): Segnala cartellini rossi, valori negativi e azioni distruttive di eliminazione.
  * Blu Accento (`#3b82f6`): Elementi attivi, selezioni, focus e pulsanti di navigazione primaria.

### 3.2 Popup Operativi e Funzionali (Dark Theme Coerente)
Tutte le finestre modali operative complesse derivate da layout FXML dedicati (`create_match_popup.fxml`, `edit_match_stats_popup.fxml`, `alias_mapping_popup.fxml`, dialoghi di selezione stagione per import CSV):
* **Condivisone del Foglio di Stile:** Includono esplicitamente `styles.css` nella propria `Scene`.
* **Coerenza Visiva:** Mantengono lo sfondo scuro, le card con bordi arrotondati e la medesima formattazione di tabelle, dropdown e campi di input, garantendo un'esperienza utente uniforme e priva di sfarfallii visivi durante il data-entry.

### 3.3 Dialoghi di Sistema, Notifica e Conferma (Light / White Theme per Massimo Contrasto)
Al contrario dei popup operativi, tutti i messaggi di sistema e i dialoghi modali basati sulla classe `javafx.scene.control.Alert` (`AlertType.CONFIRMATION`, `AlertType.WARNING`, `AlertType.ERROR`, `AlertType.INFORMATION`):
* **Stile Visivo:** Mantengono deliberatamente il **tema chiaro/bianco nativo** del sistema operativo (sfondo bianco, testo scuro ad alto contrasto, pulsanti di sistema standard).
* **Razionale UX & Psicologia dell'Interfaccia:**
  * **Interruzione ad Alta Priorità:** Un `Alert` rappresenta una richiesta di attenzione straordinaria che interrompe temporaneamente il normale flusso di lavoro (es. *«Sei sicuro di voler eliminare le quote salvate per questo match?»*, *«Confermi l'eliminazione definitiva della competizione?»*, o segnalazioni di errore bloccante nell'elaborazione del motore di calcolo).
  * **Visual Pop-Out Effect:** Il forte contrasto cromatico tra la finestra bianca luminosa e l'ambiente desktop scuro sottostante cattura istantaneamente lo sguardo del trader. Questo riduce drasticamente il rischio di distrazioni o conferme accidentali (*misclicks*) su azioni irreversibili.
* **Regola di Ancoraggio Desktop (`initOwner`):** Per garantire la stabilità modale su qualsiasi ambiente operativo (in particolare macOS e Linux), ogni istanza di `Alert` deve essere tassativamente ancorata allo stage/finestra genitore tramite `alert.initOwner(parentWindow)`. Ciò previene la perdita di focus e impedisce che la finestra modale scivoli inavvertitamente dietro l'interfaccia principale.

### 3.4 Principio di Progettazione Interfacce: Azioni Reversibili vs Dialoghi di Conferma (Prevenzione della Modal Fatigue)
NEPE adotta un rigoroso principio di ergonomia desktop per la gestione delle conferme utente (allineato alle *Apple Human Interface Guidelines* e ai principi di *Interaction Design*):

1. **Quando Usare Dialoghi Modali di Conferma (`Alert.AlertType.CONFIRMATION`):**
   * Esclusivamente per **azioni irreversibili, distruttive o ad alto impatto** che determinano l'eliminazione permanente di dati (es. *«Elimina Squadra Selezionata»*, *«Elimina Competizione»*, *«Elimina Partita»* o cancellazione massiva di quote).
   * L'interruzione modale serve a forzare una pausa decisionale deliberata (*friction by design*).

2. **Quando Evitare i Dialoghi Modali (Azioni Dirette e Reversibili):**
   * Quando un'azione è **non distruttiva, a basso impatto e immediatamente reversibile** tramite un controllo visibile nella medesima schermata (es. la disassociazione di una squadra da un campionato tramite `btnDisassociateTeam`, prontamente ripristinabile in un singolo click con l'adiacente `btnAssociateTeam`).
   * **Prevenzione della Modal Fatigue:** L'abuso di finestre modali di conferma per operazioni reversibili abitua l'utente a cliccare meccanicamente "OK", annullando l'efficacia protettiva dei dialoghi per le vere azioni distruttive.
   * **Pattern Sostitutivo Applicato:**
     * **Etichette Semantiche Chiare:** Utilizzo di verbi precisi ed espliciti (es. `"❌ Disassocia"` anziché un generico `"❌ Rimuovi"` che potrebbe essere scambiato per cancellazione anagrafica).
     * **Auto-Documentazione via Tooltip:** Presenza di un `<tooltip>` descrittivo che rassicura il trader sulle conseguenze reali (es. chiarire che la disassociazione da una lega non cancella l'anagrafica né le partite storiche dal DB).
     * **Feedback Non Bloccante:** Notifica visiva immediata dell'esito tramite etichette di stato contestuali (`lblStatus`), preservando la fluidità operativa nelle operazioni batch (es. aggiornamento delle leghe a fine stagione).
