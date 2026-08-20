-- =============================================================================
-- NEPE 2.0 - Database Schema DDL (MariaDB / MySQL)
-- Configurato per essere NON distruttivo (Persistenza dati tra i riavvii)
-- =============================================================================

-- 1. Tabella Competizioni
CREATE TABLE IF NOT EXISTS competitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    country VARCHAR(50) NOT NULL,
    dixon_coles_rho DECIMAL(5,4) NOT NULL DEFAULT -0.1200
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabella Squadre (Anagrafica ufficiale visualizzata in GUI)
CREATE TABLE IF NOT EXISTS teams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabella Alias Squadre (Mappatura nomi grezzi da CSV/Bookmaker -> Squadra Ufficiale)
CREATE TABLE IF NOT EXISTS team_aliases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    alias_name VARCHAR(100) NOT NULL UNIQUE,
    team_id INT NOT NULL,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabella Stagioni (Formato YYYY/YYYY es. 2025/2026)
CREATE TABLE IF NOT EXISTS seasons (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(9) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tabella Partite (Matches)
CREATE TABLE IF NOT EXISTS matches (
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
    
    -- Expected Goals (xG) Manual Override
    manual_home_xg DECIMAL(5,3) NULL,
    manual_away_xg DECIMAL(5,3) NULL,
    
    -- Quote Pre-Match di Riferimento da CSV/Market
    odds_home DECIMAL(6,3) NULL,
    odds_draw DECIMAL(6,3) NULL,
    odds_away DECIMAL(6,3) NULL,
    
    -- Modificatori di Contesto Pre-Match
    is_neutral_venue TINYINT(1) NOT NULL DEFAULT 0,
    must_win_home TINYINT(1) NOT NULL DEFAULT 0,
    must_win_away TINYINT(1) NOT NULL DEFAULT 0,
    low_urgency_home TINYINT(1) NOT NULL DEFAULT 0,
    low_urgency_away TINYINT(1) NOT NULL DEFAULT 0,
    
    -- Moltiplicatori Infortuni/Formazione (Attacco/Difesa)
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
    
    -- Key univoca per identificare lo stesso match durante re-import CSV (Upsert)
    UNIQUE KEY uq_match_teams_date (home_team_id, away_team_id, match_date_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indici per ottimizzare la ricerca storica del campione partite (Media xG ultimi N incontri)
-- Nota: Usiamo CREATE INDEX solo se non già gestito
CREATE INDEX IF NOT EXISTS idx_matches_date ON matches(match_date_time);
CREATE INDEX IF NOT EXISTS idx_matches_home_lookup ON matches(home_team_id, match_date_time);
CREATE INDEX IF NOT EXISTS idx_matches_away_lookup ON matches(away_team_id, match_date_time);
CREATE INDEX IF NOT EXISTS idx_matches_competition ON matches(competition_id, season_id);

-- 6. Tabella Eventi Live (Gol, Rossi)
CREATE TABLE IF NOT EXISTS match_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    event_type VARCHAR(20) NOT NULL, -- 'GOAL_HOME', 'GOAL_AWAY', 'RED_CARD_HOME', 'RED_CARD_AWAY'
    minute INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    INDEX idx_match_events_lookup (match_id, minute)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Tabella Quote Exchange Correnti (Impostate dall'utente per la valutazione EV)
CREATE TABLE IF NOT EXISTS market_odds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    match_id INT NOT NULL,
    market_type VARCHAR(30) NOT NULL, -- 'MATCH_ODDS', 'UNDER_OVER_25', 'BTTS_YES_NO', ecc.
    outcome VARCHAR(10) NOT NULL,     -- '1', 'X', '2', 'OVER', 'UNDER', 'YES', 'NO', ecc.
    back_odds DECIMAL(6,3) NULL,
    lay_odds DECIMAL(6,3) NULL,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    UNIQUE KEY uq_match_market_outcome (match_id, market_type, outcome)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Tabella Impostazioni Globali dell'Applicazione
CREATE TABLE IF NOT EXISTS settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Vista per Dettaglio Match (JOIN Denormalizzati per Read-Only GUI / DAO)
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
