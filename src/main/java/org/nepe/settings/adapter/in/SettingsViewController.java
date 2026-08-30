package org.nepe.settings.adapter.in;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.ManageSettingsUseCase;
import org.nepe.settings.port.in.UpdateSettingsCommand;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.util.Objects;

/**
 * Driving Inbound Adapter (JavaFX Controller) for Application Preferences & Global Calibration.
 * <p>
 * Manages user interactions for modifying and persisting system parameters:
 * <ul>
 *     <li>Exchange commission rate.</li>
 *     <li>Historical match sample size N.</li>
 *     <li>Inter-season decay factor gamma (γ).</li>
 *     <li>Live trading cash-out profit target.</li>
 * </ul>
 */
@Controller
public class SettingsViewController {

    private static final Logger log = LoggerFactory.getLogger(SettingsViewController.class);

    private final ManageSettingsUseCase manageSettingsUseCase;
    private final SpringFXMLLoader springFXMLLoader;

    // --- FXML Controls ---
    @FXML private Button btnBackToDashboard;
    @FXML private TextField txtCommissionRate;
    @FXML private TextField txtDefaultNMatches;
    @FXML private TextField txtSeasonalDecayGamma;
    @FXML private TextField txtGreenUpProfitTarget;
    @FXML private Label lblStatus;
    @FXML private Button btnResetDefaults;
    @FXML private Button btnSaveSettings;

    public SettingsViewController(ManageSettingsUseCase manageSettingsUseCase,
                                  SpringFXMLLoader springFXMLLoader) {
        this.manageSettingsUseCase = Objects.requireNonNull(manageSettingsUseCase, "ManageSettingsUseCase must not be null");
        this.springFXMLLoader = Objects.requireNonNull(springFXMLLoader, "SpringFXMLLoader must not be null");
    }

    @FXML
    public void initialize() {
        loadCurrentSettings();
    }

    private void loadCurrentSettings() {
        try {
            AppSettings settings = manageSettingsUseCase.getSettings();
            populateForm(settings);
        } catch (Exception e) {
            log.error("Failed to load application settings", e);
            lblStatus.setText("Errore durante il caricamento delle impostazioni: " + e.getMessage());
        }
    }

    private void populateForm(AppSettings settings) {
        if (settings == null) return;
        txtCommissionRate.setText(String.format("%.4f", settings.getCommissionRate()).replace(',', '.'));
        txtDefaultNMatches.setText(String.valueOf(settings.getDefaultNMatches()));
        txtSeasonalDecayGamma.setText(String.format("%.4f", settings.getSeasonalDecayGamma()).replace(',', '.'));
        txtGreenUpProfitTarget.setText(String.format("%.4f", settings.getGreenUpProfitTarget()).replace(',', '.'));
    }

    @FXML
    public void handleSaveSettings(ActionEvent event) {
        lblStatus.setText("");

        try {
            double commission = parseDouble(txtCommissionRate.getText(), "Tasso di commissione");
            int nMatches = parseInt(txtDefaultNMatches.getText(), "Dimensione campione N");
            double gamma = parseDouble(txtSeasonalDecayGamma.getText(), "Fattore di decadimento gamma");
            double target = parseDouble(txtGreenUpProfitTarget.getText(), "Soglia profitto Green-Up");

            UpdateSettingsCommand command = new UpdateSettingsCommand(commission, nMatches, gamma, target);
            AppSettings updated = manageSettingsUseCase.updateSettings(command);
            populateForm(updated);

            lblStatus.setText("Impostazioni salvate con successo nel database!");
        } catch (NepeException e) {
            lblStatus.setText("Errore: " + e.getMessage());
        } catch (Exception e) {
            lblStatus.setText("Errore imprevisto durante il salvataggio: " + e.getMessage());
        }
    }

    @FXML
    public void handleResetDefaults(ActionEvent event) {
        lblStatus.setText("");
        try {
            AppSettings defaults = manageSettingsUseCase.resetToDefaults();
            populateForm(defaults);
            lblStatus.setText("Impostazioni ripristinate ai valori predefiniti di sistema.");
        } catch (Exception e) {
            log.error("Failed to reset settings to defaults", e);
            lblStatus.setText("Errore durante il ripristino dei default: " + e.getMessage());
        }
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        try {
            Parent root = springFXMLLoader.load("/views/dashboard.fxml");
            Stage stage = (Stage) btnBackToDashboard.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("NEPE 2.0 - Nexus Exchange Prediction Engine");
        } catch (Exception e) {
            log.error("Failed to return to dashboard", e);
        }
    }

    // --- Parsing Helpers ---

    private static double parseDouble(String str, String fieldName) {
        if (str == null || str.isBlank()) {
            throw new DomainValidationException(fieldName + " non può essere vuoto.");
        }
        try {
            return Double.parseDouble(str.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new DomainValidationException(fieldName + " deve essere un valore numerico valido.");
        }
    }

    private static int parseInt(String str, String fieldName) {
        if (str == null || str.isBlank()) {
            throw new DomainValidationException(fieldName + " non può essere vuoto.");
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new DomainValidationException(fieldName + " deve essere un numero intero valido.");
        }
    }
}
