package org.nepe;

import javafx.application.Application;
import org.nepe.bootstrap.JavaFxApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for NEPE (Nexus Exchange Prediction Engine).
 * <p>
 * Configures the Spring Boot application context and delegates lifecycle orchestration
 * to {@link JavaFxApplication} for desktop GUI presentation.
 */
@SpringBootApplication
public class NepeApplication {

    public static void main(String[] args) {
        Application.launch(JavaFxApplication.class, args);
    }
}

/*

Esegui un audit completo e approfondito dell'intero progetto seguendo le direttive in AGENTS.md. Analizza l'architettura, i pattern applicati, la gestione delle risorse, la concorrenza e
l'efficienza. Non modificare alcun file: genera prima un report strutturato dei problemi rilevati (suddivisi per la tassonomia di gravità definita in AGENTS.md: BLOCKER, CRITICAL, MAJOR, MINOR,
IMPROVEMENT), indicando classe, riga e spiegazione logica. Trovi la documentazione in @docs (puoi tralasciare il file PDF).
Questa è la QUARTA iterazione di revisione del progetto: ti chiedo di verificare specificamente se permangono criticità di livello BLOCKER, CRITICAL o MAJOR e se il sistema soddisfa i criteri di
stabilità e convergenza architetturale.

* */