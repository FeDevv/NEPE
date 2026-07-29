# NEPE 2.0 - Specifica Funzionale dei Requisiti

Questo documento descrive in dettaglio i requisiti funzionali, le regole di business e le logiche matematiche che governano il comportamento di **NEPE 2.0**.

---

## 1. Requisiti Funzionali (RF)

I requisiti descrivono le azioni che l'utente può compiere e le risposte attese dal sistema.

### RF-01: Selezione Competizione e Caricamento Dati
* **Selezione Competizione:** L'utente può selezionare la competizione su cui operare (la **"Competizione Selezionata"**, es. Serie A) tramite un menu a tendina nella GUI.
* **Importazione CSV Manuale (Supporto Multi-Stagione):** L'utente può caricare file CSV storici scaricati da Football-Data relativi sia alla **stagione corrente** che a **stagioni passate** (es. la stagione precedente per creare il buffer statistico iniziale). Il sistema effettua il parsing e popola il database.
  * **Campi CSV mappati a DB:**
    * `Div` $\to$ Codice Competizione (es. `I1` per Serie A).
    * `Date` e `Time` (se presente) $\to$ Data e ora del match, convertite e salvate in UTC.
    * `HomeTeam`, `AwayTeam` $\to$ Nomi delle squadre (mappati tramite il sistema degli alias).
    * `FTHG`, `FTAG` $\to$ Gol segnati a tempo pieno (`home_score`, `away_score`).
    * `HS`, `AS` $\to$ Tiri totali (usati per stimare l'xG).
    * `HST`, `AST` $\to$ Tiri in porta (usati per stimare l'xG).
    * `HR`, `AR` $\to$ Cartellini rossi complessivi.
    * `B365H`, `B365D`, `B365A` (o equivalenti) $\to$ Quote pre-match di mercato per l'esito 1X2.
* **Mappatura Alias Squadre (Team Alias Mapping):** Se il sistema rileva nel CSV un nome di squadra non presente nel database, mostra un popup all'utente per permettergli di:
  1. Associare la squadra del CSV a una squadra già esistente nel DB (creando un alias permanente).
  2. Creare una nuova squadra nel DB.
  Questo garantisce la coerenza storica dei dati statistici.

### RF-02: Controllo e Operatività Manuale (CRUD)
L'applicazione deve essere operabile interamente a mano, consentendo all'utente di bypassare o correggere i dati importati:
* **Gestione Squadre e Competizioni:** Creazione, modifica ed eliminazione manuale delle anagrafiche.
* **Gestione Palinsesto (Match):** Aggiunta manuale di partite future, modifica di partite esistenti (squadre, date, stato) ed eliminazione.
* **Modifica Risultati e Statistiche:** Possibilità di inserire o modificare i punteggi finali dei match e le statistiche aggregate (tiri, tiri in porta, xG).
* **Gestione Eventi Storici:** Aggiunta, modifica o rimozione manuale degli eventi atomici legati a una partita.

### RF-03: Analisi Pre-Match e Calcolo del Valore
* **Visualizzazione Palinsesto:** Mostra l'elenco delle partite in programma per la competizione attiva.
* **Calcolo Probabilità:** Stima le probabilità reali degli esiti principali (1X2, Under/Over da 0.5 a 4.5, BTTS) usando la distribuzione di Poisson e la correzione di Dixon-Coles.
* **Inserimento Quote & Expected Value (Punta/Banca):** Consente all'utente di inserire le quote correnti per le posizioni Punta (Back) e Banca (Lay). Il sistema calcola l'Expected Value (EV) per entrambe le opzioni, integrando una percentuale di commissione dell'exchange configurabile (es. default 5%):
    * **Valore Atteso Punta con Commissione (EV Back):**
      $$\text{EV}_{\text{Punta, Comm}} = P \times (K_{\text{Back}} - 1) \times (1 - \text{comm}) - (1 - P)$$
      *Dove $P$ è la probabilità reale stimata, $K_{\text{Back}}$ è la quota Punta di mercato e $\text{comm}$ è la commissione dell'exchange.*
    * **Valore Atteso Banca con Commissione (EV Lay):**
      $$\text{EV}_{\text{Banca, Comm}} = (1 - P) \times (1 - \text{comm}) - P \times (K_{\text{Lay}} - 1)$$
      *Dove $K_{\text{Lay}}$ è la quota Banca di mercato.*
    * **Valore Atteso Banca tarato sul Rischio (EV Lay Rischio):**
      $$\text{EV}_{\text{Banca, Rischio}} = \frac{\text{EV}_{\text{Banca, Comm}}}{K_{\text{Lay}} - 1}$$
      *Questo indica l'EV rapportato alla responsabilità persa (capitale a rischio) anziché alla stake del backer.*
    Se l'EV di una quota (classico o tarato sul rischio) è positivo ($> 0$), il software la evidenzia graficamente nella GUI come un'opportunità di trading di valore.
* **Configurazione Modificatori Pre-Match:** L'utente può impostare per ciascuna partita i modificatori specifici:
  * Moltiplicatori di attacco/difesa delle singole squadre (es. per infortuni rilevanti).
  * Flag `must_win_home` e `must_win_away`.
  * Flag `low_urgency_home` e `low_urgency_away`.
  * Flag `is_neutral_venue` (per disattivare il fattore campo).

### RF-04: Trading Live e Ricalcolo Dinamico
* **Console Live:** Consente di aprire una schermata di monitoraggio per una partita in corso.
* **Inserimento Eventi Live:** L'utente aggiorna manualmente il minuto di gioco corrente e inserisce eventi specifici cliccando su pulsanti dedicati (Gol Casa, Gol Ospite, Cartellino Rosso Casa, Cartellino Rosso Ospite).
* **Ricalcolo Istantaneo:** Ad ogni aggiornamento, il sistema ricalcola istantaneamente le probabilità degli esiti residui in base al tempo mancante e allo stato corrente della partita.
* **Target di Green Up Dinamici:** La GUI suggerisce l'uscita in profitto (Green Up) in base a soglie e parametri di profitto configurabili dall'utente nelle impostazioni generali dell'applicazione.

---

## 2. Regole di Business e Formule Matematiche

### 2.1 Stima dell'xG (Expected Goals)
Football-Data non contiene metriche xG native. Per popolare le colonne `home_xg` e `away_xg` nel database:
* Il sistema applica una formula di stima basata sui tiri totali e sui tiri in porta:
  $$\text{xG Stimato} = (\text{Tiri in Porta} \times 0.30) + ((\text{Tiri Totali} - \text{Tiri in Porta}) \times 0.05)$$
* **Priorità Manuale:** Se l'utente inserisce manualmente un valore per `home_xg` o `away_xg` nella scheda della partita, il sistema ignora il calcolo automatico e adotta il valore inserito a mano.

### 2.2 Calcolo delle Forze Storiche, Fallback e Applicazione Modificatori
Per stimare la probabilità degli esiti, il sistema adotta il **Modello di Poisson Semplificato** per calcolare la forza offensiva ($\alpha_i$) e difensiva ($\beta_i$) di ciascuna squadra, basandosi sulle partite recenti (casa e trasferta) con un peso decrescente nel tempo (*Recency Weighting*).

* **Logica di Selezione del Campione Partite:**
  1. **Tutte le partite della stagione corrente:** Se la squadra ha disputato $M \ge 10$ partite nella stagione corrente, il sistema analizza **tutte le $M$ partite** giocate finora nella stagione attiva.
  2. **Garantire il Campione Minimo ($N_{\text{min}} = 10$):** Se nella stagione corrente la squadra ha disputato meno di 10 partite ($M < 10$), il sistema recupera automaticamente dal database le ultime $10 - M$ partite disputate da quella squadra nella **stagione precedente**, arrestando il recupero al raggiungimento della soglia minima di 10 partite complessive.
* **Metriche di Riferimento:** Si utilizzano primariamente gli **Expected Goals (xG)** stimati (o inseriti manualmente). I gol reali vengono usati come fallback solo se i dati sui tiri non sono presenti nel database.
* **Ponderazione e Decadimento Temporale (Recency Weighting):**
  Alle partite più vecchie viene applicato un peso di decadimento proporzionale alla loro anzianità. La media degli xG non è una semplice media aritmetica, bensì una **media pesata**:
  $$\bar{xG}_{\text{segnati}, i} = \frac{\sum_{k=1}^K w_k \cdot \text{xG}_{\text{segnati}, k}}{\sum_{k=1}^K w_k}$$
  *dove $w_k$ è il peso assegnato alla partita $k$ (con peso massimo per il match più recente e peso decrescente per i più vecchi).*
  * Per le partite appartenenti alla **stagione precedente**, al peso di anzianità si sovrappone anche il **fattore di decadimento stagionale $\gamma = 0.70$** (configurabile) per riflettere i cambi di rosa/allenatore tra le due stagioni.
* **Formule delle Forze:**
  $$\alpha_i = \frac{\bar{xG}_{\text{segnati}, i}}{\bar{xG}_{\text{campionato}}}, \quad \beta_i = \frac{\bar{xG}_{\text{subiti}, i}}{\bar{xG}_{\text{campionato}}}$$
* **Calcolo dei Goal Attesi Pre-Match ($\lambda$ e $\mu$):**
  $$\lambda_{\text{Home}} = \alpha_{\text{Home}} \times \beta_{\text{Away}} \times \text{Home Advantage}$$
  $$\mu_{\text{Away}} = \alpha_{\text{Away}} \times \beta_{\text{Home}} \times \text{Away Disadvantage (pari a } 1/\text{Home Advantage)}$$
  *Dove l'Home Advantage è il rapporto tra la media xG segnati in casa e la media xG segnati in trasferta nell'intera competizione.*
* **Applicazione dei Modificatori di Contesto:** I modificatori (infortuni, Must-Win, Low-Urgency) si applicano direttamente ai tassi di gol attesi pre-match ($\lambda_{\text{Home}}$ e $\mu_{\text{Away}}$) della partita corrente prima di calcolare la matrice dei punteggi esatti.

### 2.3 Modificatori Live e di Contesto
* **Time-Decay (Decadimento Temporale):** Il tasso di gol attesi per il tempo rimanente si riduce in modo lineare rispetto al novantesimo minuto:
  $$\lambda_{\text{residuo}} = \lambda_{\text{iniziale}} \times \left( \frac{90 - \text{Minuto Corrente}}{90} \right)$$
  Oltre il 90° minuto (recupero), la stima dei gol attesi residui si azzera (clamp a 0), in quanto il trading live sui minuti di recupero presenta rischi elevatissimi e si opera sui 90 minuti standard.
* **Cartellino Rosso Live (Live Red Card Corrector):** Se una squadra riceve uno o più cartellini rossi durante il live, il modificatore si applica in modo cumulativo e moltiplicativo per ogni espulsione ($C$ rappresenta il numero di rossi della squadra):
  * Il tasso di gol attesi residui della squadra espulsa viene ridotto del 30% per ciascun rosso: $\lambda_{\text{espulso, residuo}} = \lambda_{\text{espulso, residuo}} \times 0.70^C$
  * Il tasso di gol attesi dell'avversario viene incrementato del 30% per ciascun rosso: $\lambda_{\text{avversario, residuo}} = \lambda_{\text{avversario, residuo}} \times 1.30^C$
* **Regola Motivazionale Must-Win Live:** A partire dal secondo tempo ($\text{Minuto} \ge 45$), se una squadra ha il flag `must_win` attivo ed è in situazione di pareggio o svantaggio:
  * Il suo tasso offensivo aumenta del 25%: $\lambda_{\text{must\_win, residuo}} = \lambda_{\text{must\_win, residuo}} \times 1.25$
  * La sua difesa si sbilancia, aumentando il tasso di gol attesi dell'avversario del 20%: $\lambda_{\text{avversario, residuo}} = \lambda_{\text{avversario, residuo}} \times 1.20$
* **Regola Low-Urgency:** Se entrambe le squadre sono contrassegnate come `low_urgency` (lo scenario in cui un pareggio favorisce la qualificazione o salvezza di entrambe):
  * I tassi di gol attesi di entrambe le squadre vengono ridotti del 35% per rispecchiare l'atteggiamento prudente sul campo: $\lambda \times 0.65$.

### 2.4 Modello Matematico di Dixon-Coles
La distribuzione di Poisson standard assume che i gol delle due squadre siano indipendenti. Tuttavia, nel calcio i punteggi bassi presentano una forte correlazione (maggiore frequenza di 0-0 e 1-1, minore frequenza di 1-0 e 0-1). 
Il modello di Dixon-Coles corregge la matrice dei punteggi esatti $P(X=x, Y=y)$ per i punteggi più bassi applicando un fattore di aggiustamento $\tau(x, y)$:
$$P(X=x, Y=y) = \tau(x, y) \times \frac{\lambda^x e^{-\lambda}}{x!} \times \frac{\mu^y e^{-\mu}}{y!}$$
Dove:
* $\lambda$ è il tasso di gol attesi della squadra in casa (Home).
* $\mu$ è il tasso di gol attesi della squadra ospite (Away).
* $\tau(x, y)$ è definito come:
  * $\text{Se } (x,y) = (0,0) \implies \tau(0,0) = 1 - \lambda \mu \rho$
  * $\text{Se } (x,y) = (1,0) \implies \tau(1,0) = 1 + \mu \rho$
  * $\text{Se } (x,y) = (0,1) \implies \tau(0,1) = 1 + \lambda \rho$
  * $\text{Se } (x,y) = (1,1) \implies \tau(1,1) = 1 - \rho$
  * $\text{In tutti gli altri casi } \implies \tau(x,y) = 1$
* $\rho$ è il coefficiente di correzione specifico per la competizione (salvato nella tabella `competitions` del DB; valore di default globale pari a $-0.12$).

---

## 3. Gestione Fusi Orari e Date

Per evitare fraintendimenti o sfasamenti orari nell'analisi pre-match e live:
* **Persistenza (DB):** Tutte le date e gli orari di inizio dei match inseriti nel database sono espressi in formato **UTC**.
* **Visualizzazione (GUI):** In fase di presentazione a schermo, la GUI converte automaticamente l'orario UTC nel fuso orario locale del sistema dell'utente (es. **CET/CEST** per l'Italia).

---

## 4. Architettura e Stack Tecnologico

Il sistema è sviluppato seguendo un approccio robusto ed enterprise-grade per garantire manutenibilità e isolamento della logica di calcolo:
* **Linguaggio:** Java 25 (con supporto a Virtual Threads, Records e Pattern Matching).
* **Framework Backend:** Spring Boot (Core, Dependency Injection).
* **Interfaccia Grafica:** JavaFX (GUI desktop nativa, integrata nel contesto Spring Boot).
* **Database:** MariaDB (installazione locale per persistenza relazionale tramite HikariCP).
* **Connection Pool:** HikariCP.
* **Logging:** SLF4J con Logback.
* **Testing:** JUnit 5.
* **Analisi Statica:** Configurazione SonarQube integrata per il monitoraggio del debito tecnico.
* **Architettura Software:** **Architettura Esagonale** (Ports and Adapters).
  * Il *Domain Core* contiene la logica matematica pura (Poisson, Dixon-Coles, calcolo EV, modificatori live) ed è privo di dipendenze esterne verso Spring, JavaFX o MariaDB.
  * Gli *Adapters* gestiscono la GUI (JavaFX), la persistenza (MariaDB tramite DAO verticali e DTO) e l'importatore CSV.
* **Organizzazione del Codice:** Approccio **feature-by-package** combinato con l'architettura esagonale per mantenere elevata coesione e basso accoppiamento.
* **Operatività Manuale Totale:** Il sistema fornisce un'interfaccia CRUD completa per operare manualmente su qualsiasi entità nel database (aggiungere/modificare/eliminare squadre, competizioni, partite, punteggi, statistiche ed eventi live), permettendo di ignorare o correggere i dati caricati tramite CSV.
