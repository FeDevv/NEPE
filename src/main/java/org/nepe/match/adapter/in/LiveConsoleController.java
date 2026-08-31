package org.nepe.match.adapter.in;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.inference.port.in.CalculateLiveInferenceUseCase;
import org.nepe.inference.port.in.LiveAnalysisResult;
import org.nepe.inference.port.in.LiveInferenceQuery;
import org.nepe.inference.port.in.MarketPrediction;
import org.nepe.match.domain.*;
import org.nepe.match.port.in.LiveMatchTradingUseCase;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.in.RecordMatchEventCommand;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.ManageSettingsUseCase;
import org.nepe.shared.exception.LiveTradingException;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Driving Inbound Adapter (JavaFX Controller) for the Real-Time In-Game Trading Console.
 * <p>
 * Orchestrates live event ingestion and dynamic probability recalibration:
 * <ul>
 *     <li>Records live atomic in-game events (Goals, Red Cards) with immediate aggregate updates.</li>
 *     <li>Supports instantaneous event rollbacks (Undo Last Event).</li>
 *     <li>Progresses match elapsed minutes and applies linear Time-Decay ($(90-t)/90$).</li>
 *     <li>Applies cumulative red card penalties ($0.70^C$ / $1.30^C$) and second-half Must-Win motivation.</li>
 *     <li>Triggers dynamic Green-Up / Cash Out alerts when market prices satisfy user profit targets.</li>
 * </ul>
 */
@Controller
public class LiveConsoleController {

    private static final Logger log = LoggerFactory.getLogger(LiveConsoleController.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Rome"));

    private final LiveMatchTradingUseCase liveMatchTradingUseCase;
    private final CalculateLiveInferenceUseCase calculateLiveInferenceUseCase;
    private final ManageMatchUseCase manageMatchUseCase;
    private final ManageMarketOddsUseCase manageMarketOddsUseCase;
    private final ManageSettingsUseCase manageSettingsUseCase;
    private final SpringFXMLLoader springFXMLLoader;

    // --- FXML Header Controls ---
    @FXML private Button btnBackToDashboard;
    @FXML private ComboBox<MatchDetailsDTO> comboLiveMatchSelector;
    @FXML private Label lblMatchHeader;
    @FXML private Label lblMatchInfo;
    @FXML private Button btnStartLive;
    @FXML private Button btnFinishMatch;

    // --- FXML Scoreboard & Minute Stepper ---
    @FXML private Label lblHomeTeamName;
    @FXML private Label lblScore;
    @FXML private Label lblAwayTeamName;

    @FXML private Button btnMinus1Min;
    @FXML private TextField txtCurrentMinute;
    @FXML private Button btnPlus1Min;
    @FXML private Button btnPlus5Min;

    @FXML private Label lblHomeRedCards;
    @FXML private Label lblAwayRedCards;

    // --- FXML Event Action Buttons ---
    @FXML private Button btnGoalHome;
    @FXML private Button btnGoalAway;
    @FXML private Button btnRedCardHome;
    @FXML private Button btnRedCardAway;
    @FXML private Button btnUndoLastEvent;

    // --- FXML Green-Up Banner ---
    @FXML private HBox boxGreenUpBanner;
    @FXML private Label lblGreenUpText;

    // --- FXML Residual Probability Grid ---
    @FXML private Label lblResidualRates;
    @FXML private Label lblLiveProb1;
    @FXML private Label lblLiveProbX;
    @FXML private Label lblLiveProb2;
    @FXML private Label lblLiveProbUnder25;
    @FXML private Label lblLiveProbOver25;
    @FXML private Label lblLiveProbBtts;

    @FXML private Label lblLiveFairOdds1;
    @FXML private Label lblLiveFairOddsX;
    @FXML private Label lblLiveFairOdds2;
    @FXML private Label lblLiveFairOddsUnder25;
    @FXML private Label lblLiveFairOddsOver25;
    @FXML private Label lblLiveFairOddsBtts;

    // --- FXML Events Chronology Log & Status ---
    @FXML private ListView<String> lstEventsLog;
    @FXML private Label lblStatus;

    // --- Live State ---
    private MatchDetailsDTO currentMatch;
    private int currentHomeScore = 0;
    private int currentAwayScore = 0;
    private int currentHomeRedCards = 0;
    private int currentAwayRedCards = 0;
    private int currentMinute = 0;
    private AppSettings currentSettings;

    public LiveConsoleController(LiveMatchTradingUseCase liveMatchTradingUseCase,
                                 CalculateLiveInferenceUseCase calculateLiveInferenceUseCase,
                                 ManageMatchUseCase manageMatchUseCase,
                                 ManageMarketOddsUseCase manageMarketOddsUseCase,
                                 ManageSettingsUseCase manageSettingsUseCase,
                                 SpringFXMLLoader springFXMLLoader) {
        this.liveMatchTradingUseCase = Objects.requireNonNull(liveMatchTradingUseCase, "LiveMatchTradingUseCase must not be null");
        this.calculateLiveInferenceUseCase = Objects.requireNonNull(calculateLiveInferenceUseCase, "CalculateLiveInferenceUseCase must not be null");
        this.manageMatchUseCase = Objects.requireNonNull(manageMatchUseCase, "ManageMatchUseCase must not be null");
        this.manageMarketOddsUseCase = Objects.requireNonNull(manageMarketOddsUseCase, "ManageMarketOddsUseCase must not be null");
        this.manageSettingsUseCase = Objects.requireNonNull(manageSettingsUseCase, "ManageSettingsUseCase must not be null");
        this.springFXMLLoader = Objects.requireNonNull(springFXMLLoader, "SpringFXMLLoader must not be null");
    }

    @FXML
    public void initialize() {
        this.currentSettings = manageSettingsUseCase.getSettings();
        configureMatchDropdown();
        configureMinuteInput();
        loadAvailableLiveMatches();
    }

    private void configureMatchDropdown() {
        comboLiveMatchSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(MatchDetailsDTO match) {
                return (match != null) ?
                        String.format("%s vs %s (%s)", match.homeTeamName(), match.awayTeamName(), match.matchState().name()) : "";
            }

            @Override
            public MatchDetailsDTO fromString(String string) {
                return null;
            }
        });

        comboLiveMatchSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldMatch, newMatch) -> {
            if (newMatch != null) {
                loadMatchDetails(newMatch);
            }
        });
    }

    private void configureMinuteInput() {
        txtCurrentMinute.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isBlank()) {
                try {
                    int parsed = Integer.parseInt(newVal.trim());
                    if (parsed >= 0 && parsed <= 130 && parsed != currentMinute) {
                        this.currentMinute = parsed;
                        if (currentMatch != null) {
                            liveMatchTradingUseCase.updateLiveMinute(currentMatch.matchId(), currentMinute);
                        }
                        recalculateLiveInference();
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });
    }

    private void loadAvailableLiveMatches() {
        try {
            List<MatchDetailsDTO> matches = manageMatchUseCase.getMatchDetailsByState(1, 1, MatchState.LIVE);
            if (matches.isEmpty()) {
                matches = manageMatchUseCase.getMatchDetailsByState(1, 1, MatchState.SCHEDULED);
            }

            comboLiveMatchSelector.setItems(FXCollections.observableArrayList(matches));
            if (!matches.isEmpty()) {
                comboLiveMatchSelector.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            log.warn("Could not preload matches for live trading console", e);
        }
    }

    /**
     * Sets the active competition and season scope to populate live match selector.
     */
    public void setScope(int competitionId, int seasonId) {
        try {
            List<MatchDetailsDTO> liveMatches = manageMatchUseCase.getMatchDetailsByState(competitionId, seasonId, MatchState.LIVE);
            comboLiveMatchSelector.setItems(FXCollections.observableArrayList(liveMatches));
            if (!liveMatches.isEmpty()) {
                comboLiveMatchSelector.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            log.warn("Could not load live matches for competition ID {} and season ID {}", competitionId, seasonId, e);
        }
    }

    /**
     * Binds a match aggregate to the live console view.
     *
     * @param match match details DTO
     */
    public void loadMatchDetails(MatchDetailsDTO match) {
        if (match == null) return;
        this.currentMatch = match;

        lblHomeTeamName.setText(match.homeTeamName());
        lblAwayTeamName.setText(match.awayTeamName());

        this.currentHomeScore = (match.homeScore() != null) ? match.homeScore() : 0;
        this.currentAwayScore = (match.awayScore() != null) ? match.awayScore() : 0;
        this.currentHomeRedCards = match.homeRedCards();
        this.currentAwayRedCards = match.awayRedCards();
        this.currentMinute = match.currentMinute();

        updateScoreboardDisplay();

        lblMatchHeader.setText(String.format("%s vs %s", match.homeTeamName(), match.awayTeamName()));
        lblMatchInfo.setText(String.format("%s | Inizio: %s (CET) | Stato: %s",
                match.competitionName(),
                formatDateTime(match.matchDateTime()),
                match.matchState().name()));

        reloadEventsHistory();
        recalculateLiveInference();
    }

    private void updateScoreboardDisplay() {
        lblScore.setText(String.format("%d - %d", currentHomeScore, currentAwayScore));
        txtCurrentMinute.setText(String.valueOf(currentMinute));
        lblHomeRedCards.setText(String.format("🟥 Rossi Casa: %d", currentHomeRedCards));
        lblAwayRedCards.setText(String.format("🟥 Rossi Ospite: %d", currentAwayRedCards));
    }

    private void reloadEventsHistory() {
        if (currentMatch == null) return;
        try {
            List<MatchEvent> events = liveMatchTradingUseCase.getMatchEvents(currentMatch.matchId());
            List<String> formatted = events.stream()
                    .map(e -> String.format("[%d'] %s", e.getMinute(), formatEventType(e.getEventType())))
                    .toList();
            lstEventsLog.setItems(FXCollections.observableArrayList(formatted));
        } catch (Exception e) {
            log.error("Could not load match events log", e);
        }
    }

    private String formatEventType(MatchEventType type) {
        return switch (type) {
            case GOAL_HOME -> "⚽ GOL SQUADRA CASA";
            case GOAL_AWAY -> "⚽ GOL SQUADRA OSPITE";
            case RED_CARD_HOME -> "🟥 CARTELLINO ROSSO CASA";
            case RED_CARD_AWAY -> "🟥 CARTELLINO ROSSO OSPITE";
        };
    }

    // --- Core Real-Time In-Game Probability Recalculation ---

    private void recalculateLiveInference() {
        if (currentMatch == null) return;

        try {
            int defaultN = (currentSettings != null) ? currentSettings.getDefaultNMatches() : 10;
            double gamma = (currentSettings != null) ? currentSettings.getSeasonalDecayGamma() : 0.70;

            // 1. Fetch team historical match performances (N_min = 10 with previous season gamma decay)
            List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> homeHistory =
                    manageMatchUseCase.getHistoricalTeamPerformances(currentMatch.homeTeamId(), currentMatch.competitionId(), currentMatch.seasonId(), defaultN);
            List<org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance> awayHistory =
                    manageMatchUseCase.getHistoricalTeamPerformances(currentMatch.awayTeamId(), currentMatch.competitionId(), currentMatch.seasonId(), defaultN);

            double leagueAvgXg = manageMatchUseCase.getLeagueAverageXgPerTeam(currentMatch.competitionId(), currentMatch.seasonId());

            org.nepe.inference.domain.TeamStrengthCalculator.TeamStrength homeStrength =
                    org.nepe.inference.domain.TeamStrengthCalculator.calculateStrength(homeHistory, leagueAvgXg, gamma);
            org.nepe.inference.domain.TeamStrengthCalculator.TeamStrength awayStrength =
                    org.nepe.inference.domain.TeamStrengthCalculator.calculateStrength(awayHistory, leagueAvgXg, gamma);

            double homeAdv = currentMatch.isNeutralVenue() ? 1.0 : 1.20;

            MatchModifiers modifiers = new MatchModifiers(
                    currentMatch.isNeutralVenue(),
                    currentMatch.isMustWinHome(),
                    currentMatch.isMustWinAway(),
                    currentMatch.isLowUrgencyHome(),
                    currentMatch.isLowUrgencyAway(),
                    currentMatch.modAttHome(),
                    currentMatch.modDefHome(),
                    currentMatch.modAttAway(),
                    currentMatch.modDefAway()
            );

            org.nepe.inference.domain.TeamStrengthCalculator.PreMatchRates preRates =
                    org.nepe.inference.domain.TeamStrengthCalculator.calculatePreMatchRates(
                            homeStrength, awayStrength, leagueAvgXg, homeAdv, modifiers
                    );

            double lambdaPre = preRates.lambdaHome();
            double muPre = preRates.muAway();

            double commission = (currentSettings != null) ? currentSettings.getCommissionRate() : 0.05;
            double profitTarget = (currentSettings != null) ? currentSettings.getGreenUpProfitTarget() : 0.10;

            List<MarketOdds> liveOdds = manageMarketOddsUseCase.getOddsForMatch(currentMatch.matchId());

            LiveInferenceQuery query = new LiveInferenceQuery(
                    lambdaPre,
                    muPre,
                    currentMinute,
                    currentHomeScore,
                    currentAwayScore,
                    currentHomeRedCards,
                    currentAwayRedCards,
                    modifiers,
                    currentMatch.dixonColesRho(),
                    commission,
                    profitTarget,
                    liveOdds,
                    currentMatch.oddsHome()
            );

            LiveAnalysisResult result = calculateLiveInferenceUseCase.calculate(query);

            // Update residual rates display
            lblResidualRates.setText(String.format("λ residuo: %.2f | μ residuo: %.2f (Minuto: %d')",
                    result.lambdaHomeResidual(), result.muAwayResidual(), result.currentMinute()));

            // Update Probabilities and Fair Odds
            updateLivePrediction(result.finalHomeWin(), lblLiveProb1, lblLiveFairOdds1);
            updateLivePrediction(result.finalDraw(), lblLiveProbX, lblLiveFairOddsX);
            updateLivePrediction(result.finalAwayWin(), lblLiveProb2, lblLiveFairOdds2);

            if (result.underOverPredictions().size() >= 6) {
                updateLivePrediction(result.underOverPredictions().get(4), lblLiveProbUnder25, lblLiveFairOddsUnder25);
                updateLivePrediction(result.underOverPredictions().get(5), lblLiveProbOver25, lblLiveFairOddsOver25);
            }

            updateLivePrediction(result.bttsYes(), lblLiveProbBtts, lblLiveFairOddsBtts);

            // Update Green-Up Alert Banner
            boolean greenUpActive = result.greenUpTargetMet();
            boxGreenUpBanner.setVisible(greenUpActive);
            boxGreenUpBanner.setManaged(greenUpActive);

            lblStatus.setText("");
        } catch (Exception e) {
            log.error("Error calculating live probability inferences", e);
            lblStatus.setText("Errore calcolo probabilità live: " + e.getMessage());
        }
    }

    private void updateLivePrediction(MarketPrediction pred, Label lblProb, Label lblFair) {
        if (pred == null) return;
        lblProb.setText(String.format("%.1f%%", pred.probability() * 100.0));
        lblFair.setText(String.format("Quota: %.2f", pred.fairOdds()));
    }

    // --- Minute Stepper Handlers ---

    @FXML
    public void handlePlus1Min(ActionEvent event) {
        advanceMinute(1);
    }

    @FXML
    public void handlePlus5Min(ActionEvent event) {
        advanceMinute(5);
    }

    @FXML
    public void handleMinus1Min(ActionEvent event) {
        advanceMinute(-1);
    }

    private void advanceMinute(int delta) {
        int newMin = Math.max(0, Math.min(130, this.currentMinute + delta));
        this.currentMinute = newMin;
        txtCurrentMinute.setText(String.valueOf(newMin));
    }

    // --- In-Game Event Handlers ---

    @FXML
    public void handleGoalHome(ActionEvent event) {
        recordLiveEvent(MatchEventType.GOAL_HOME);
    }

    @FXML
    public void handleGoalAway(ActionEvent event) {
        recordLiveEvent(MatchEventType.GOAL_AWAY);
    }

    @FXML
    public void handleRedCardHome(ActionEvent event) {
        recordLiveEvent(MatchEventType.RED_CARD_HOME);
    }

    @FXML
    public void handleRedCardAway(ActionEvent event) {
        recordLiveEvent(MatchEventType.RED_CARD_AWAY);
    }

    private void recordLiveEvent(MatchEventType type) {
        if (currentMatch == null) return;

        try {
            liveMatchTradingUseCase.recordEvent(new RecordMatchEventCommand(
                    currentMatch.matchId(),
                    type,
                    currentMinute
            ));

            // Update local memory state
            switch (type) {
                case GOAL_HOME -> currentHomeScore++;
                case GOAL_AWAY -> currentAwayScore++;
                case RED_CARD_HOME -> currentHomeRedCards++;
                case RED_CARD_AWAY -> currentAwayRedCards++;
            }

            updateScoreboardDisplay();
            reloadEventsHistory();
            recalculateLiveInference();
        } catch (NepeException e) {
            lblStatus.setText("Impossibile registrare l'evento: " + e.getMessage());
        }
    }

    @FXML
    public void handleUndoLastEvent(ActionEvent event) {
        if (currentMatch == null) return;

        try {
            Match revertedMatch = liveMatchTradingUseCase.revertLastEvent(currentMatch.matchId());

            this.currentHomeScore = (revertedMatch.getStatistics().getHomeScore() != null) ? revertedMatch.getStatistics().getHomeScore() : 0;
            this.currentAwayScore = (revertedMatch.getStatistics().getAwayScore() != null) ? revertedMatch.getStatistics().getAwayScore() : 0;
            this.currentHomeRedCards = revertedMatch.getStatistics().getHomeRedCards();
            this.currentAwayRedCards = revertedMatch.getStatistics().getAwayRedCards();

            updateScoreboardDisplay();
            reloadEventsHistory();
            recalculateLiveInference();
            lblStatus.setText("Ultimo evento annullato con successo.");
        } catch (LiveTradingException e) {
            lblStatus.setText(e.getMessage());
        } catch (Exception e) {
            lblStatus.setText("Errore durante l'annullamento: " + e.getMessage());
        }
    }

    @FXML
    public void handleStartLive(ActionEvent event) {
        if (currentMatch == null) return;

        try {
            liveMatchTradingUseCase.startLiveTrading(currentMatch.matchId());
            lblStatus.setText("Partita avviata in modalità LIVE.");
        } catch (Exception e) {
            lblStatus.setText("Errore avvio live: " + e.getMessage());
        }
    }

    @FXML
    public void handleFinishMatch(ActionEvent event) {
        if (currentMatch == null) return;

        try {
            liveMatchTradingUseCase.finishLiveMatch(currentMatch.matchId());
            lblStatus.setText("Partita conclusa e salvata.");
        } catch (Exception e) {
            lblStatus.setText("Errore conclusione partita: " + e.getMessage());
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

    private static String formatDateTime(Instant instant) {
        return (instant != null) ? DATE_TIME_FORMATTER.format(instant) : "-";
    }
}
