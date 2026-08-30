package org.nepe;

import javafx.application.Application;
import org.nepe.bootstrap.JavaFxApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for NEPE 2.0 (Nexus Exchange Prediction Engine).
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

// agy --conversation=9441f379-2469-48b7-8778-00e22f38eb22