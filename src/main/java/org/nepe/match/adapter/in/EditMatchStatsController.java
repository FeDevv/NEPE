package org.nepe.match.adapter.in;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.nepe.inference.domain.XgEstimator;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.in.UpdateMatchStatisticsCommand;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Driving Inbound Adapter (JavaFX Modal Controller) for manually updating or overriding match statistics and xG.
 * <p>
 * Orchestrates user inputs for manual scoreline, shot count, red cards, and xG correction:
 * <ul>
 *     <li>Loads existing match statistics and manual xG overrides.</li>
 *     <li>Computes real-time dynamic heuristic xG previews using {@link XgEstimator}.</li>
 *     <li>Validates domain invariants (scores &gt;= 0, shotsOnTarget &lt;= totalShots, redCards in [0, 5]).</li>
 *     <li>Dispatches {@link UpdateMatchStatisticsCommand} and triggers overwrite protection (is_manually_edited = 1).</li>
 * </ul>
 */
@Controller
public class EditMatchStatsController {

    private static final Logger log = LoggerFactory.getLogger(EditMatchStatsController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Rome"));

    private final ManageMatchUseCase manageMatchUseCase;

    // --- FXML UI Injections ---
    @FXML private Label lblMatchTitle;
    @FXML private Label lblMatchInfo;
    @FXML private Label lblHomeHeader;
    @FXML private Label lblAwayHeader;

    @FXML private TextField txtHomeScore;
    @FXML private TextField txtAwayScore;
    @FXML private TextField txtHomeShots;
    @FXML private TextField txtAwayShots;
    @FXML private TextField txtHomeShotsOnTarget;
    @FXML private TextField txtAwayShotsOnTarget;
    @FXML private TextField txtHomeRedCards;
    @FXML private TextField txtAwayRedCards;

    @FXML private Label lblPreviewHomeXg;
    @FXML private Label lblPreviewAwayXg;
    @FXML private TextField txtManualHomeXg;
    @FXML private TextField txtManualAwayXg;

    @FXML private Label lblError;
    @FXML private Button btnResetManualXg;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    // --- State ---
    private MatchDetailsDTO currentMatch;
    private boolean statsUpdated = false;

    public EditMatchStatsController(ManageMatchUseCase manageMatchUseCase) {
        this.manageMatchUseCase = Objects.requireNonNull(manageMatchUseCase, "ManageMatchUseCase must not be null");
    }

    @FXML
    public void initialize() {
        configureRealTimeXgPreviewListeners();
    }

    private void configureRealTimeXgPreviewListeners() {
        txtHomeShots.textProperty().addListener((obs, oldVal, newVal) -> updateHomeXgPreview());
        txtHomeShotsOnTarget.textProperty().addListener((obs, oldVal, newVal) -> updateHomeXgPreview());

        txtAwayShots.textProperty().addListener((obs, oldVal, newVal) -> updateAwayXgPreview());
        txtAwayShotsOnTarget.textProperty().addListener((obs, oldVal, newVal) -> updateAwayXgPreview());
    }

    /**
     * Initializes the modal dialog with the current match details and existing statistics.
     *
     * @param match the denormalized match details DTO
     */
    public void setMatch(MatchDetailsDTO match) {
        if (match == null) return;
        this.currentMatch = match;
        this.statsUpdated = false;

        lblMatchTitle.setText(String.format("%s vs %s", match.homeTeamName(), match.awayTeamName()));
        lblMatchInfo.setText(String.format("%s (%s) | %s (CET) | Stato: %s",
                match.competitionName(),
                match.seasonName(),
                formatDateTime(match.matchDateTime()),
                match.matchState().name()));

        lblHomeHeader.setText(match.homeTeamName());
        lblAwayHeader.setText(match.awayTeamName());

        // Populate scores
        txtHomeScore.setText(match.homeScore() != null ? String.valueOf(match.homeScore()) : "");
        txtAwayScore.setText(match.awayScore() != null ? String.valueOf(match.awayScore()) : "");

        // Populate shots
        txtHomeShots.setText(match.homeShots() != null ? String.valueOf(match.homeShots()) : "");
        txtAwayShots.setText(match.awayShots() != null ? String.valueOf(match.awayShots()) : "");
        txtHomeShotsOnTarget.setText(match.homeShotsOnTarget() != null ? String.valueOf(match.homeShotsOnTarget()) : "");
        txtAwayShotsOnTarget.setText(match.awayShotsOnTarget() != null ? String.valueOf(match.awayShotsOnTarget()) : "");

        // Populate red cards
        txtHomeRedCards.setText(String.valueOf(match.homeRedCards()));
        txtAwayRedCards.setText(String.valueOf(match.awayRedCards()));

        // Populate manual xG
        txtManualHomeXg.setText(match.manualHomeXg() != null ? String.format(Locale.US, "%.3f", match.manualHomeXg()) : "");
        txtManualAwayXg.setText(match.manualAwayXg() != null ? String.format(Locale.US, "%.3f", match.manualAwayXg()) : "");

        lblError.setText("");

        // Trigger dynamic xG calculation
        updateHomeXgPreview();
        updateAwayXgPreview();
    }

    private void updateHomeXgPreview() {
        Integer shots = parseIntegerOrNull(txtHomeShots.getText());
        Integer target = parseIntegerOrNull(txtHomeShotsOnTarget.getText());

        if (shots != null && target != null && shots >= 0 && target >= 0 && target <= shots) {
            double estimated = XgEstimator.estimate(shots, target);
            lblPreviewHomeXg.setText(String.format(Locale.US, "%.3f", estimated));
        } else {
            lblPreviewHomeXg.setText("-");
        }
    }

    private void updateAwayXgPreview() {
        Integer shots = parseIntegerOrNull(txtAwayShots.getText());
        Integer target = parseIntegerOrNull(txtAwayShotsOnTarget.getText());

        if (shots != null && target != null && shots >= 0 && target >= 0 && target <= shots) {
            double estimated = XgEstimator.estimate(shots, target);
            lblPreviewAwayXg.setText(String.format(Locale.US, "%.3f", estimated));
        } else {
            lblPreviewAwayXg.setText("-");
        }
    }

    // --- Action Handlers ---

    @FXML
    public void handleResetManualXg(ActionEvent event) {
        txtManualHomeXg.setText("");
        txtManualAwayXg.setText("");
        lblError.setText("");
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        this.statsUpdated = false;
        closeModal();
    }

    @FXML
    public void handleSave(ActionEvent event) {
        if (currentMatch == null) return;
        lblError.setText("");

        try {
            String homeName = currentMatch.homeTeamName();
            String awayName = currentMatch.awayTeamName();

            // 1. Parse and validate scores
            Integer homeScore = parseInteger(txtHomeScore.getText(), "Gol (" + homeName + ")");
            Integer awayScore = parseInteger(txtAwayScore.getText(), "Gol (" + awayName + ")");
            if (homeScore != null && homeScore < 0) {
                throw new DomainValidationException(String.format("I gol di %s non possono essere negativi.", homeName));
            }
            if (awayScore != null && awayScore < 0) {
                throw new DomainValidationException(String.format("I gol di %s non possono essere negativi.", awayName));
            }

            // 2. Parse and validate shots
            Integer homeShots = parseInteger(txtHomeShots.getText(), "Tiri Totali (" + homeName + ")");
            Integer awayShots = parseInteger(txtAwayShots.getText(), "Tiri Totali (" + awayName + ")");
            Integer homeShotsOnTarget = parseInteger(txtHomeShotsOnTarget.getText(), "Tiri in Porta (" + homeName + ")");
            Integer awayShotsOnTarget = parseInteger(txtAwayShotsOnTarget.getText(), "Tiri in Porta (" + awayName + ")");

            validateShotCounts(homeShots, homeShotsOnTarget, homeName);
            validateShotCounts(awayShots, awayShotsOnTarget, awayName);

            // 3. Parse and validate red cards
            Integer homeRedCards = parseInteger(txtHomeRedCards.getText(), "Cartellini Rossi (" + homeName + ")");
            Integer awayRedCards = parseInteger(txtAwayRedCards.getText(), "Cartellini Rossi (" + awayName + ")");
            validateRedCards(homeRedCards, homeName);
            validateRedCards(awayRedCards, awayName);

            // 4. Parse and validate manual xG
            Double manualHomeXg = parseDouble(txtManualHomeXg.getText(), "Override xG (" + homeName + ")");
            Double manualAwayXg = parseDouble(txtManualAwayXg.getText(), "Override xG (" + awayName + ")");
            if (manualHomeXg != null && (manualHomeXg < 0.0 || Double.isNaN(manualHomeXg) || Double.isInfinite(manualHomeXg))) {
                throw new DomainValidationException(String.format("Il valore xG manuale di %s deve essere un numero finito >= 0.", homeName));
            }
            if (manualAwayXg != null && (manualAwayXg < 0.0 || Double.isNaN(manualAwayXg) || Double.isInfinite(manualAwayXg))) {
                throw new DomainValidationException(String.format("Il valore xG manuale di %s deve essere un numero finito >= 0.", awayName));
            }

            // 5. Construct command and dispatch to UseCase
            UpdateMatchStatisticsCommand command = new UpdateMatchStatisticsCommand(
                    currentMatch.matchId(),
                    homeScore,
                    awayScore,
                    homeShots,
                    awayShots,
                    homeShotsOnTarget,
                    awayShotsOnTarget,
                    (homeRedCards != null) ? homeRedCards : 0,
                    (awayRedCards != null) ? awayRedCards : 0,
                    manualHomeXg,
                    manualAwayXg
            );

            manageMatchUseCase.updateStatistics(command);
            log.info("Successfully updated match statistics and manual xG for match ID {}", currentMatch.matchId());

            this.statsUpdated = true;
            closeModal();

        } catch (DomainValidationException e) {
            lblError.setText(e.getMessage());
        } catch (NepeException e) {
            lblError.setText("Errore di dominio: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error saving match statistics", e);
            lblError.setText("Errore imprevisto durante il salvataggio: " + e.getMessage());
        }
    }

    private static void validateShotCounts(Integer total, Integer onTarget, String teamName) {
        if (total != null && total < 0) {
            throw new DomainValidationException(String.format("I tiri totali di %s non possono essere negativi.", teamName));
        }
        if (onTarget != null && onTarget < 0) {
            throw new DomainValidationException(String.format("I tiri in porta di %s non possono essere negativi.", teamName));
        }
        if (total == null && onTarget != null && onTarget > 0) {
            throw new DomainValidationException(String.format("Specificare i tiri totali di %s se sono presenti tiri in porta.", teamName));
        }
        if (total != null && onTarget != null && onTarget > total) {
            throw new DomainValidationException(String.format("I tiri in porta di %s (%d) non possono superare i tiri totali (%d).",
                    teamName, onTarget, total));
        }
    }

    private static void validateRedCards(Integer redCards, String teamName) {
        if (redCards != null && (redCards < 0 || redCards > 5)) {
            throw new DomainValidationException(String.format("I cartellini rossi di %s devono essere compresi tra 0 e 5.", teamName));
        }
    }

    private void closeModal() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    // --- Helpers ---

    private static Integer parseInteger(String str, String fieldName) {
        if (str == null || str.isBlank()) return null;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            throw new DomainValidationException(String.format("Il valore inserito per '%s' non è un numero intero valido.", fieldName));
        }
    }

    private static Double parseDouble(String str, String fieldName) {
        if (str == null || str.isBlank()) return null;
        try {
            return Double.parseDouble(str.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new DomainValidationException(String.format("Il valore inserito per '%s' non è un numero decimale valido.", fieldName));
        }
    }

    private static Integer parseIntegerOrNull(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatDateTime(Instant instant) {
        return (instant != null) ? DATE_TIME_FORMATTER.format(instant) : "-";
    }

    public boolean isStatsUpdated() {
        return statsUpdated;
    }
}
