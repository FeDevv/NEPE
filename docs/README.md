# NEPE 2.0 - Nexus Exchange Prediction Engine

Benvenuto in **NEPE 2.0**, la piattaforma software progettata per supportare e ottimizzare le decisioni operative nei mercati calcistici del Betting Exchange (Punta/Banca). 

NEPE 2.0 funge da assistente analitico personale: stima le probabilità reali degli eventi calcistici incrociando lo storico delle prestazioni delle squadre e ti permette di confrontarle con le quote offerte dai mercati per individuare opportunità a valore matematico positivo (Expected Value), sia prima dell'inizio delle partite che in tempo reale.

Il sistema adotta una filosofia **Human-in-the-Loop** (l'utente al centro del flusso): offre una totale flessibilità di manipolazione manuale dei dati unita a strumenti di caricamento semplificato per azzerare lo sforzo operativo.

---

## 📋 Cosa è in grado di fare il programma

### 📂 Gestione della Competizione e dei Dati
* **Selezione della Competizione:** Consente di focalizzare l'intera operatività del software su un'unica competizione in quel momento selezionata dall'utente (la **"Competizione Selezionata"**, es. Serie A, Premier League), concentrando i calcoli ed evitando distrazioni.
* **Importazione dei Dati Storici:** Permette di caricare i file di dati (in formato CSV) relativi alla **stagione corrente/attiva** scaricati autonomamente dal sito pubblico e gratuito Football-Data. Con un solo caricamento settimanale, il database locale viene popolato con lo storico dei risultati, delle statistiche di gioco (tiri, tiri in porta, cartellini) e delle quote di mercato recenti.
* **Database Locale Persistente:** Mantiene in memoria tutte le informazioni importate o create per la stagione attiva, in modo da avere sempre a disposizione lo storico necessario per stimare i rapporti di forza tra le squadre.

### ✍️ Controllo e Operatività Manuale Totale
Il software non è una scatola chiusa ed è interamente governabile a mano per far fronte a qualsiasi evenienza o correzione:
* **Gestione Match:** Consente di aggiungere manualmente nuove partite, modificare i dettagli di quelle esistenti (date, orari, squadre) o eliminare match dal palinsesto.
* **Modifica dei Punteggi e delle Statistiche:** Permette di inserire o correggere i punteggi finali delle partite terminate e le statistiche associate (come il conteggio dei tiri o degli Expected Goals).
* **Gestione delle Anagrafiche:** Consente di creare, modificare o eliminare squadre e competizioni direttamente dall'interfaccia.

### 🎯 Analisi Pre-Match e Individuazione del Valore
* **Visualizzazione del Palinsesto:** Mostra un quadro ordinato e chiaro di tutte le partite in programma per la competizione attiva.
* **Stima delle Probabilità Pure:** Calcola ed espone le percentuali reali di accadimento per i principali mercati di scambio:
  * *Match Odds (1X2):* Vittoria in casa, pareggio, vittoria in trasferta.
  * *Under / Over X.5:* Soglie da 0.5 a 4.5 gol complessivi.
  * *Both Teams to Score (BTTS):* Entrambe le squadre a segno (Sì / No).
* **Inserimento Quote e Calcolo dell'Expected Value (EV):** Consente di inserire le quote di mercato correnti (Punta o Banca). Il sistema le confronta istantaneamente con le probabilità stimate e calcola la bontà matematica della giocata, evidenziando le opportunità in cui la quota di mercato è più alta del dovuto.
* **Modificatori di Partita:** Permette di regolare la forza delle squadre prima dell'incontro impostando parametri specifici:
  * *Assenze ed Infortuni:* Moltiplicatori manuali per penalizzare o potenziare l'attacco o la difesa di una squadra a causa di assenze chiave.
  * *Stato Must-Win:* Segnalazione di squadre che necessitano assolutamente della vittoria, influenzando il loro atteggiamento tattico.
  * *Stato Low-Urgency:* Segnalazione di contesti in cui un pareggio è comodo a entrambe le formazioni (riducendo l'aspettativa di gioco aggressivo).

### ⚡ Trading Live e Ricalcolo in Tempo Reale
Per le partite seguite in diretta, il programma si trasforma in una console di controllo live interattiva:
* **Inserimento Eventi in Tempo Reale:** Consente all'utente di registrare istantaneamente la segnatura di un gol (specificando la squadra) o l'assegnazione di un cartellino rosso.
* **Gestione del Minuto di Gioco:** Permette di aggiornare costantemente il minuto corrente della partita.
* **Ricalcolo Istantaneo delle Probabilità:** Ad ogni variazione del minuto o all'inserimento di un evento, il sistema aggiorna in frazioni di secondo le probabilità dei gol residui tenendo conto del tempo che scorre (decadimento temporale), delle espulsioni (superiorità/inferiorità numerica) e della pressione di classifica (Must-Win nel secondo tempo).
* **Supporto alle Strategie di Uscita (Green Up / Cash Out):** Fornisce dati aggiornati per decidere se e quando uscire anticipatamente dal mercato per bloccare un profitto o limitare una perdita.

---

## 🛠️ Stack Tecnologico e Architettura
* **Core Language:** Java 25 (sfruttando Virtual Threads, Records e Pattern Matching per massima efficienza e leggibilità).
* **Framework:** Spring Boot (Iniezione delle dipendenze, gestione del ciclo di vita dei componenti).
* **Interfaccia Grafica:** JavaFX (GUI nativa fluida, integrata con Spring Boot).
* **Database:** MariaDB locale (connesso tramite il pool di connessioni **HikariCP**).
* **Architettura:** **Architettura Esagonale (Ports and Adapters)** organizzata secondo un approccio **feature-by-package**. Il nucleo dei calcoli matematici (Inference Engine) è totalmente isolato da infrastrutture, database e GUI, garantendo testabilità ottimale.
* **Qualità del Codice:** Sviluppo guidato da test con **JUnit 5** e monitoraggio del debito tecnico tramite **SonarQube**.
* **Version control:** GitHub.

---

## 📅 Roadmap del Progetto
* **Fase 1 (Attuale):** Sviluppo del database, dell'importatore manuale dei file CSV, dell'Inference Engine per il calcolo delle probabilità/valore pre-match, dei modificatori di partita e della GUI per il monitoraggio ed inserimento eventi live.
* **Fase 2 (Futura):** Modulo per la gestione automatizzata del portafoglio (Money Management), calcolo dello stake ideale e storico dei profitti e delle perdite personali.
