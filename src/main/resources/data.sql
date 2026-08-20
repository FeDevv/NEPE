-- =============================================================================
-- NEPE 2.0 - Initial Seed Data (DML)
-- =============================================================================

-- Inserimento impostazioni di default iniziale (utilizzando INSERT IGNORE per idempotenza)
INSERT IGNORE INTO settings (setting_key, setting_value) VALUES 
('commission_rate', '0.05'),
('default_n_matches', '10'),
('seasonal_decay_gamma', '0.70'),
('green_up_profit_target', '0.10');
