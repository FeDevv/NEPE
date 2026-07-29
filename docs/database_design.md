# Progettazione del Database - NEPE 2.0

Questo documento descrive la struttura del database relazionale locale per **NEPE 2.0** utilizzando **MariaDB**. Lo schema è progettato per rispettare rigorosamente la **Terza Forma Normale (3NF)**, garantendo l'assenza di ridondanze, l'integrità dei dati e ottime prestazioni di query tramite indici ottimizzati.

Include inoltre la strategia per il caricamento ripetuto dei file CSV e la tutela dei dati modificati manualmente dall'utente.

---

## 1. Analisi e Mapping dei Dati (Football-Data CSV)

Confrontando le colonne del file `football-data.txt` con le necessità del motore matematico (Poisson, Dixon-Coles, xG) e i requisiti funzionali, abbiamo individuato le seguenti associazioni:

| Colonna CSV | Destinazione Logica / Tabella | Note |
| :--- | :--- | :--- |
| `Div` | `competitions.code` | Identificativo della competizione (es. `I1` per Serie A). |
| `Date` + `Time` | `matches.match_date_time` | Uniti e convertiti in `DATETIME` UTC. Se `Time` manca, si assume `12:00:00` UTC. |
| `HomeTeam` | `team_aliases.alias_name` | Mappato sul relativo `team_id` tramite alias case-insensitive. |
| `AwayTeam` | `team_aliases.alias_name` | Mappato sul relativo `team_id` tramite alias case-insensitive. |
| `FTHG` (o `HG`) | `matches.home_score` | Gol segnati a tempo pieno dalla squadra in casa. |
| `FTAG` (o `AG`) | `matches.away_score` | Gol segnati a tempo pieno dalla squadra ospite. |
| `HS` / `AS` | `matches.home_shots` / `matches.away_shots` | Tiri totali (usati per stimare l'xG). |
| `HST` / `AST` | `matches.home_shots_on_target` / `matches.away_shots_on_target` | Tiri in porta (usati per stimare l'xG). |
| `HR` / `AR` | `matches.home_red_cards` / `matches.away_red_cards` | Cartellini rossi della partita. |
| `AvgH`/`AvgD`/`AvgA` | `matches.odds_home` / `_draw` / `_away` | Quote medie di mercato 1X2 (in alternativa `B365H`/`D`/`A` come fallback). |

---

## 2. Gestione del Re-Import CSV e "Overwrite Protection"

Quando l'utente scarica un CSV aggiornato della stagione corrente e lo ricarica nel programma, dobbiamo evitare di creare duplicati o di sovrascrivere le modifiche effettuate manualmente a schermo (es. inserimento o correzione manuale di punteggi/statistiche).

### 2.1 Identificazione Unica dei Match (Upsert Key)
Football-Data non fornisce un ID univoco. Il sistema identifica univocamente una partita tramite la chiave composita:
$$\{\text{home\_team\_id}, \text{away\_team\_id}, \text{data\_match (solo giorno, yyyy-mm-dd)}\}$$
Due squadre non possono giocare l'una contro l'altra due volte nello stesso giorno, il che rende questa chiave sicura ed univoca.

### 2.2 Strategia di Aggiornamento (Upsert)
Durante il parsing del CSV, per ogni riga:
1. Se il match **non esiste** nel DB, viene inserito un nuovo record.
2. Se il match **esiste già** nel DB:
   * **Se `is_manually_edited = 1`:** Il sistema **salta** l'aggiornamento dei dati di punteggio, tiri e cartellini per non perdere le correzioni manuali inserite dall'utente (*Human-in-the-Loop*). Viene aggiornata solo la quota se prima era assente.
   * **Se `is_manually_edited = 0`:** Il sistema effettua l'aggiornamento dei dati (es. se la partita era programmata e ora è terminata, vengono compilati punteggi e statistiche).

---

## 3. Schema Logico del Database (3NF)

### 3.1 Tabella `competitions`
* **`id`** (INT, PK, Auto-Increment)
* **`code`** (VARCHAR(10), Unique): Codice Football-Data (es. `I1`, `E0`).
* **`name`** (VARCHAR(100)): Nome descrittivo (es. "Serie A").
* **`country`** (VARCHAR(50)): Nazione.
* **`dixon_coles_rho`** (DECIMAL(5,4), Default -0.12): Parametro di correzione dei punteggi bassi.

### 3.2 Tabella `teams`
* **`id`** (INT, PK, Auto-Increment)
* **`name`** (VARCHAR(100), Unique): Nome ufficiale visualizzato nella GUI.

### 3.3 Tabella `team_aliases`
* **`id`** (INT, PK, Auto-Increment)
* **`alias_name`** (VARCHAR(100), Unique): Nome alternativo (es. "Man City" per "Manchester City").
* **`team_id`** (INT, FK -> `teams.id` ON DELETE CASCADE)

### 3.4 Tabella `seasons`
* **`id`** (INT, PK, Auto-Increment)
* **`name`** (VARCHAR(9), Unique): Formato `YYYY/YYYY` (es. `2025/2026`).

### 3.5 Tabella `matches`
* **`id`** (INT, PK, Auto-Increment)
* **`season_id`** (INT, FK -> `seasons.id`)
* **`competition_id`** (INT, FK -> `competitions.id`)
* **`home_team_id`** (INT, FK -> `teams.id`)
* **`away_team_id`** (INT, FK -> `teams.id`)
* **`match_date_time`** (DATETIME, UTC): Data e ora d'inizio.
* **`state`** (VARCHAR(20)): Stato del match (`SCHEDULED`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED`).
* **`is_manually_edited`** (TINYINT(1), Default 0): Flag impostato a 1 se l'utente ha modificato manualmente i dati.
* **`home_score`** (INT, Nullable)
* **`away_score`** (INT, Nullable)
* **`home_shots`** (INT, Nullable)
* **`away_shots`** (INT, Nullable)
* **`home_shots_on_target`** (INT, Nullable)
* **`away_shots_on_target`** (INT, Nullable)
* **`home_red_cards`** (INT, Default 0)
* **`away_red_cards`** (INT, Default 0)
* **`manual_home_xg`** (DECIMAL(5,3), Nullable): Override manuale dell'utente.
* **`manual_away_xg`** (DECIMAL(5,3), Nullable): Override manuale dell'utente.
* **`odds_home`** (DECIMAL(6,3), Nullable): Quota 1 pre-match di riferimento.
* **`odds_draw`** (DECIMAL(6,3), Nullable): Quota X pre-match di riferimento.
* **`odds_away`** (DECIMAL(6,3), Nullable): Quota 2 pre-match di riferimento.
* **`is_neutral_venue`** (BOOLEAN, Default False)
* **`must_win_home`** (BOOLEAN, Default False)
* **`must_win_away`** (BOOLEAN, Default False)
* **`low_urgency_home`** (BOOLEAN, Default False)
* **`low_urgency_away`** (BOOLEAN, Default False)
* **`mod_att_home`** (DECIMAL(3,2), Default 1.00)
* **`mod_def_home`** (DECIMAL(3,2), Default 1.00)
* **`mod_att_away`** (DECIMAL(3,2), Default 1.00)
* **`mod_def_away`** (DECIMAL(3,2), Default 1.00)
* **`current_minute`** (INT, Default 0): Minuto corrente.

### 3.6 Tabella `match_events`
* **`id`** (INT, PK, Auto-Increment)
* **`match_id`** (INT, FK -> `matches.id` ON DELETE CASCADE)
* **`event_type`** (VARCHAR(20)): Tipo di evento (`GOAL_HOME`, `GOAL_AWAY`, `RED_CARD_HOME`, `RED_CARD_AWAY`).
* **`minute`** (INT): Minuto dell'evento.
* **`created_at`** (TIMESTAMP, Default CURRENT_TIMESTAMP)

### 3.7 Tabella `market_odds`
* **`id`** (INT, PK, Auto-Increment)
* **`match_id`** (INT, FK -> `matches.id` ON DELETE CASCADE)
* **`market_type`** (VARCHAR(30)): Tipo mercato (es. `MATCH_ODDS`, `UNDER_OVER_25`, `BTTS`).
* **`outcome`** (VARCHAR(10)): Esito specifico (es. `1`, `X`, `2`, `OVER`, `UNDER`, `YES`, `NO`).
* **`back_odds`** (DECIMAL(6,3), Nullable)
* **`lay_odds`** (DECIMAL(6,3), Nullable)

### 3.8 Tabella `settings`
* **`setting_key`** (VARCHAR(50), PK)
* **`setting_value`** (VARCHAR(255))

---

## 4. Analisi di Normalizzazione, Viste e Trigger

### 4.1 Normalizzazione in 3NF
1. **Prima Forma Normale (1NF):** Tabelle con PK atomiche definite, nessun valore ripetuto o multivalore.
2. **Seconda Forma Normale (2NF):** Nessuna dipendenza parziale (le chiavi esterne e gli attributi dipendono dall'intero ID surrogato delle tabelle).
3. **Terza Forma Normale (3NF):** Nessuna dipendenza transitiva.
   * *xG dinamico:* Non salviamo nel database l'xG calcolato (il quale dipenderebbe da `home_shots` e `home_shots_on_target`), ma manteniamo solo i tiri e il flag di override manuale. Java si occupa del calcolo logico a runtime.
   * *Relazione quote exchange:* La tabella `market_odds` isola le quote inserite verticalmente su base `(match_id, market_type, outcome)` impedendo ridondanze in `matches`.

### 4.2 Utilizzo di Viste
Per semplificare lo sviluppo in Java ed evitare di dover scrivere query con complessi `JOIN` multipli nel codice dell'applicazione (per recuperare i nomi dei team e i campionati), implementiamo la vista **`v_matches_details`**.
Questa vista unisce `matches` con `teams`, `competitions` e `seasons`, fornendo un record denormalizzato *solo in lettura* pronto all'uso per i DAO di Java e per popolare direttamente le tabelle nella GUI JavaFX.

### 4.3 Scelta Architetturale su Trigger vs Logica Java
In merito ai **Trigger** (es. aggiornare automaticamente il punteggio in `matches` all'inserimento di un evento in `match_events`):
* **Cosa farebbe un Trigger:** Un trigger `AFTER INSERT ON match_events` potrebbe aggiornare `home_score`/`away_score` o `home_red_cards`/`away_red_cards` su `matches`.
* **Perché lo evitiamo in NEPE 2.0 (Consigliato):** Seguendo le linee guida dell'**Architettura Esagonale** adottata, la logica di business e le transizioni di stato (es. *"se c'è un gol, il punteggio aumenta"*) devono risiedere interamente nel **Domain Core** in Java. Delegare parte di questa logica al database (trigger) frammenterebbe le regole di business, renderebbe il codice core dipendente da comportamenti impliciti del DB e complicherebbe la testabilità unitaria.
* **Soluzione:** La gestione e la sincronizzazione dello stato delle partite live è gestita interamente a livello Java, eseguendo le scritture di eventi e l'aggiornamento del match all'interno di una singola transazione gestita da Spring. Il database rimane un archivio di persistenza passivo, preservando la purezza architetturale.

---

## 5. DDL SQL per MariaDB

```sql
-- Creazione del Database (se non esiste)
CREATE DATABASE IF NOT EXISTS nepe_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nepe_db;

-- Disabilitazione temporanea dei vincoli per un drop pulito e riproducibile
SET FOREIGN_KEY_CHECKS = 0;

-- Rimozione tabelle e viste esistenti
DROP VIEW IF EXISTS v_matches_details;
DROP TABLE IF EXISTS settings;
DROP TABLE IF EXISTS market_odds;
DROP TABLE IF EXISTS match_events;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS team_aliases;
DROP TABLE IF EXISTS seasons;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS competitions;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Tabella Competizioni
CREATE TABLE competitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50) NOT NULL,
    dixon_coles_rho DECIMAL(5,4) NOT NULL DEFAULT -0.1200
) ENGINE=InnoDB;

-- 2. Tabella Squadre
CREATE TABLE teams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 3. Tabella Alias Squadre
CREATE TABLE team_aliases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    alias_name VARCHAR(100) NOT NULL UNIQUE,
    team_id INT NOT NULL,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Tabella Stagioni
CREATE TABLE seasons (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(9) NOT NULL UNIQUE -- Formato "YYYY/YYYY"
) ENGINE=InnoDB;

-- 5. Tabella Partite (Matches)
CREATE TABLE matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    season_id INT NOT NULL,
    competition_id INT NOT NULL,
    home_team_id INT NOT NULL,
    away_team_id INT NOT NULL,
    match_date_time DATETIME NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'LIVE', 'FINISHED', 'POSTPONED', 'CANCELLED'
    is_manually_edited TINYINT(1) NOT NULL DEFAULT 0,
    
    -- Risultati e Statistiche
    home_score INT NULL,
    away_score INT NULL,
    home_shots INT NULL,
    away_shots INT NULL,
    home_shots_on_target INT NULL,
    away_shots_on_target INT NULL,
    home_red_cards INT NOT NULL DEFAULT 0,
    away_red_cards INT NOT NULL DEFAULT 0,
    
    -- xG Override Manuale
    manual_home_xg DECIMAL(5,3) NULL,
    manual_away_xg DECIMAL(5,3) NULL,
    
    -- Quote Pre-Match di Riferimento
    odds_home DECIMAL(6,3) NULL,
    odds_draw DECIMAL(6,3) NULL,
    odds_away DECIMAL(6,3) NULL,
    
    -- Modificatori di Contesto Pre-Match
    is_neutral_venue TINYINT(1) NOT NULL DEFAULT 0,
    must_win_home TINYINT(1) NOT NULL DEFAULT 0,
    must_win_away TINYINT(1) NOT NULL DEFAULT 0,
    low_urgency_home TINYINT(1) NOT NULL DEFAULT 0,
    low_urgency_away TINYINT(1) NOT NULL DEFAULT 0,
    
    -- Moltiplicatori Infortuni/Formazione
    mod_att_home DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    mod_def_home DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    mod_att_away DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    mod_def_away DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    
    -- Stato Live
    current_minute INT NOT NULL DEFAULT 0,
    
    FOREIGN KEY (season_id) REFERENCES seasons(id) ON DELETE RESTRICT,
    FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE RESTRICT,
    FOREIGN KEY (home_team_id) REFERENCES teams(id) ON DELETE RESTRICT,
    FOREIGN KEY (away_team_id) REFERENCES teams(id) ON DELETE RESTRICT,
    
    -- Vincolo di unicità logica per evitare duplicati all'importazione ripetuta del CSV
    UNIQUE KEY uq_match_teams_date (home_team_id, away_team_id, match_date_time)
) ENGINE=InnoDB;

-- Indici per velocizzare il calcolo delle Forze Storiche (Media xG ultimi N incontri)
CREATE INDEX idx_matches_date ON matches(match_date_time);
CREATE INDEX idx_matches_home_lookup ON matches(home_team_id, match_date_time);
CREATE INDEX idx_matches_away_lookup ON matches(away_team_id, match_date_time);
CREATE INDEX idx_matches_competition ON matches(competition_id, season_id);

-- 6. Tabella Eventi Live
CREATE TABLE match_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    event_type VARCHAR(20) NOT NULL, -- 'GOAL_HOME', 'GOAL_AWAY', 'RED_CARD_HOME', 'RED_CARD_AWAY'
    minute INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    INDEX idx_match_events_lookup (match_id, minute)
) ENGINE=InnoDB;

-- 7. Tabella Quote Exchange Correnti (Inserite dall'utente per analisi)
CREATE TABLE market_odds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    market_type VARCHAR(30) NOT NULL, -- 'MATCH_ODDS', 'UNDER_OVER_25', 'BTTS_YES_NO', etc.
    outcome VARCHAR(10) NOT NULL,     -- '1', 'X', '2', 'OVER', 'UNDER', 'YES', 'NO', etc.
    back_odds DECIMAL(6,3) NULL,
    lay_odds DECIMAL(6,3) NULL,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    UNIQUE KEY uq_match_market_outcome (match_id, market_type, outcome)
) ENGINE=InnoDB;

-- 8. Tabella Impostazioni Globali
CREATE TABLE settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

-- 9. Creazione Vista per Dettaglio Match (JOIN Denormalizzati per Read-Only GUI/DAO)
CREATE OR REPLACE VIEW v_matches_details AS
SELECT 
    m.id AS match_id,
    m.match_date_time,
    m.state AS match_state,
    m.is_manually_edited,
    m.home_score,
    m.away_score,
    m.home_shots,
    m.away_shots,
    m.home_shots_on_target,
    m.away_shots_on_target,
    m.home_red_cards,
    m.away_red_cards,
    m.manual_home_xg,
    m.manual_away_xg,
    m.odds_home,
    m.odds_draw,
    m.odds_away,
    m.is_neutral_venue,
    m.must_win_home,
    m.must_win_away,
    m.low_urgency_home,
    m.low_urgency_away,
    m.mod_att_home,
    m.mod_def_home,
    m.mod_att_away,
    m.mod_def_away,
    m.current_minute,
    c.id AS competition_id,
    c.code AS competition_code,
    c.name AS competition_name,
    c.country AS competition_country,
    c.dixon_coles_rho,
    s.id AS season_id,
    s.name AS season_name,
    t_home.id AS home_team_id,
    t_home.name AS home_team_name,
    t_away.id AS away_team_id,
    t_away.name AS away_team_name
FROM matches m
JOIN competitions c ON m.competition_id = c.id
JOIN seasons s ON m.season_id = s.id
JOIN teams t_home ON m.home_team_id = t_home.id
JOIN teams t_away ON m.away_team_id = t_away.id;

-- Inserimento impostazioni di default iniziale
INSERT INTO settings (setting_key, setting_value) VALUES 
('commission_rate', '0.05'),
('default_n_matches', '10'),
('seasonal_decay_gamma', '0.70'),
('green_up_profit_target', '0.10');
```
