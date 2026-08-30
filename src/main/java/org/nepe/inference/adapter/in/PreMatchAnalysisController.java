package org.nepe.inference.adapter.in;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.inference.port.in.MarketPrediction;
import org.nepe.inference.port.in.PreMatchAnalysisResult;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.in.SaveMarketOddsCommand;
import org.nepe.match.port.in.UpdateMatchCommand;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.ManageSettingsUseCase;
import org.nepe.shared.exception.NepeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Driving Inbound Adapter (JavaFX Controller) for Pre-Match Quantitative Analysis and Value Hunting.
 * <p>
 * Connects the JavaFX user interface to the {@link CalculatePreMatchInferenceUseCase}:
 * <ul>
 *     <li>Loads match fixtures and past exchange market quotes.</li>
 *     <li>Applies live sliders and tactical modifiers to offensive/defensive ratings.</li>
 *     <li>Renders bivariate Dixon-Coles probability distributions and fair bookmaker odds.</li>
 *     <li>Dynamically computes Betting Exchange Expected Values (EV Back, EV Lay, and Risk-Adjusted EV Lay).</li>
 *     <li>Highlights positive mathematical value opportunities (EV &gt; 0) with green pill badges.</li>
 * </ul>
 */
@Controller
public class PreMatchAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(PreMatchAnalysisController.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Rome"));

    private final CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase;
    private final ManageMatchUseCase manageMatchUseCase;
    private final ManageMarketOddsUseCase manageMarketOddsUseCase;
    private final ManageSettingsUseCase manageSettingsUseCase;
    private final SpringFXMLLoader springFXMLLoader;

    // --- FXML Header & Navigation Controls ---
    @FXML private Button btnBackToDashboard;
    @FXML private ComboBox<MatchDetailsDTO> comboMatchSelector;
    @FXML private Label lblMatchHeader;
    @FXML private Label lblMatchInfo;
    @FXML private Button btnSaveOdds;

    // --- FXML Goal Rates & Modifiers ---
    @FXML private Label lblLambdaHome;
    @FXML private Label lblMuAway;
    @FXML private Label lblDixonColesRho;

    @FXML private Label lblModAttHomeVal;
    @FXML private Label lblModDefHomeVal;
    @FXML private Label lblModAttAwayVal;
    @FXML private Label lblModDefAwayVal;

    @FXML private Slider sliderModAttHome;
    @FXML private Slider sliderModDefHome;
    @FXML private Slider sliderModAttAway;
    @FXML private Slider sliderModDefAway;

    @FXML private CheckBox chkMustWinHome;
    @FXML private CheckBox chkMustWinAway;
    @FXML private CheckBox chkLowUrgencyHome;
    @FXML private CheckBox chkLowUrgencyAway;
    @FXML private CheckBox chkNeutralVenue;
    @FXML private Button btnResetModifiers;

    // --- FXML Market Odds: 1X2 ---
    @FXML private Label lblProb1;
    @FXML private Label lblFairOdds1;
    @FXML private TextField txtBack1;
    @FXML private TextField txtLay1;
    @FXML private Label lblEvBack1;
    @FXML private Label lblEvLay1;

    @FXML private Label lblProbX;
    @FXML private Label lblFairOddsX;
    @FXML private TextField txtBackX;
    @FXML private TextField txtLayX;
    @FXML private Label lblEvBackX;
    @FXML private Label lblEvLayX;

    @FXML private Label lblProb2;
    @FXML private Label lblFairOdds2;
    @FXML private TextField txtBack2;
    @FXML private TextField txtLay2;
    @FXML private Label lblEvBack2;
    @FXML private Label lblEvLay2;

    // --- FXML Market Odds: Under/Over 2.5 ---
    @FXML private Label lblProbUnder25;
    @FXML private Label lblFairOddsUnder25;
    @FXML private TextField txtBackUnder25;
    @FXML private TextField txtLayUnder25;
    @FXML private Label lblEvBackUnder25;
    @FXML private Label lblEvLayUnder25;

    @FXML private Label lblProbOver25;
    @FXML private Label lblFairOddsOver25;
    @FXML private TextField txtBackOver25;
    @FXML private TextField txtLayOver25;
    @FXML private Label lblEvBackOver25;
    @FXML private Label lblEvLayOver25;

    // --- FXML Market Odds: BTTS ---
    @FXML private Label lblProbBttsYes;
    @FXML private Label lblFairOddsBttsYes;
    @FXML private TextField txtBackBttsYes;
    @FXML private TextField txtLayBttsYes;
    @FXML private Label lblEvBackBttsYes;
    @FXML private Label lblEvLayBttsYes;

    @FXML private Label lblProbBttsNo;
    @FXML private Label lblFairOddsBttsNo;
    @FXML private TextField txtBackBttsNo;
    @FXML private TextField txtLayBttsNo;
    @FXML private Label lblEvBackBttsNo;
    @FXML private Label lblEvLayBttsNo;

    @FXML private Label lblStatus;

    // --- Current State ---
    private MatchDetailsDTO currentMatch;
    private AppSettings currentSettings;
    private boolean isUpdatingUi = false;

    public PreMatchAnalysisController(CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase,
                                      ManageMatchUseCase manageMatchUseCase,
                                      ManageMarketOddsUseCase manageMarketOddsUseCase,
                                      ManageSettingsUseCase manageSettingsUseCase,
                                      SpringFXMLLoader springFXMLLoader) {
        this.calculatePreMatchInferenceUseCase = Objects.requireNonNull(calculatePreMatchInferenceUseCase, "CalculatePreMatchInferenceUseCase must not be null");
        this.manageMatchUseCase = Objects.requireNonNull(manageMatchUseCase, "ManageMatchUseCase must not be null");
        this.manageMarketOddsUseCase = Objects.requireNonNull(manageMarketOddsUseCase, "ManageMarketOddsUseCase must not be null");
        this.manageSettingsUseCase = Objects.requireNonNull(manageSettingsUseCase, "ManageSettingsUseCase must not be null");
        this.springFXMLLoader = Objects.requireNonNull(springFXMLLoader, "SpringFXMLLoader must not be null");
    }

    @FXML
    public void initialize() {
        this.currentSettings = manageSettingsUseCase.getSettings();
        configureMatchDropdown();
        configureSliderBindings();
        configureInputListeners();
        loadAvailableMatches();
    }

    private void configureMatchDropdown() {
        comboMatchSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(MatchDetailsDTO match) {
                return (match != null) ?
                        String.format("%s vs %s (%s)", match.homeTeamName(), match.awayTeamName(), match.competitionCode()) : "";
            }

            @Override
            public MatchDetailsDTO fromString(String string) {
                return null;
            }
        });

        comboMatchSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldMatch, newMatch) -> {
            if (newMatch != null) {
                loadMatchDetails(newMatch);
            }
        });
    }

    private void configureSliderBindings() {
        sliderModAttHome.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblModAttHomeVal.setText(String.format("%.2f", newVal.doubleValue()));
            triggerRecalculation();
        });

        sliderModDefHome.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblModDefHomeVal.setText(String.format("%.2f", newVal.doubleValue()));
            triggerRecalculation();
        });

        sliderModAttAway.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblModAttAwayVal.setText(String.format("%.2f", newVal.doubleValue()));
            triggerRecalculation();
        });

        sliderModDefAway.valueProperty().addListener((obs, oldVal, newVal) -> {
            lblModDefAwayVal.setText(String.format("%.2f", newVal.doubleValue()));
            triggerRecalculation();
        });
    }

    private void configureInputListeners() {
        chkMustWinHome.selectedProperty().addListener((obs, o, n) -> triggerRecalculation());
        chkMustWinAway.selectedProperty().addListener((obs, o, n) -> triggerRecalculation());
        chkLowUrgencyHome.selectedProperty().addListener((obs, o, n) -> triggerRecalculation());
        chkLowUrgencyAway.selectedProperty().addListener((obs, o, n) -> triggerRecalculation());
        chkNeutralVenue.selectedProperty().addListener((obs, o, n) -> triggerRecalculation());

        // Text fields listeners for real-time EV updates
        List<TextField> allTextFields = List.of(
                txtBack1, txtLay1, txtBackX, txtLayX, txtBack2, txtLay2,
                txtBackUnder25, txtLayUnder25, txtBackOver25, txtLayOver25,
                txtBackBttsYes, txtLayBttsYes, txtBackBttsNo, txtLayBttsNo
        );

        for (TextField tf : allTextFields) {
            tf.textProperty().addListener((obs, oldVal, newVal) -> triggerRecalculation());
        }
    }

    private void loadAvailableMatches() {
        try {
            // Load matches for the first competition or current active season
            List<MatchDetailsDTO> matches = manageMatchUseCase.getMatchDetailsByCompetitionAndSeason(1, 1);
            if (matches.isEmpty()) {
                matches = manageMatchUseCase.getMatchDetailsByState(1, 1, null);
            }

            comboMatchSelector.setItems(FXCollections.observableArrayList(matches));
            if (!matches.isEmpty()) {
                comboMatchSelector.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            log.warn("Could not preload matches for pre-match analysis", e);
        }
    }

    /**
     * Loads a specific match fixture and populates its initial probability and market state.
     *
     * @param match match details DTO
     */
    public void loadMatchDetails(MatchDetailsDTO match) {
        if (match == null) return;
        this.currentMatch = match;
        this.isUpdatingUi = true;

        try {
            lblMatchHeader.setText(String.format("%s vs %s", match.homeTeamName(), match.awayTeamName()));
            lblMatchInfo.setText(String.format("%s | %s (CET) | Stato: %s",
                    match.competitionName(),
                    formatDateTime(match.matchDateTime()),
                    match.matchState().name()));

            lblDixonColesRho.setText(String.format("%.4f", match.dixonColesRho()));

            // Load modifiers from match
            sliderModAttHome.setValue(match.modAttHome());
            sliderModDefHome.setValue(match.modDefHome());
            sliderModAttAway.setValue(match.modAttAway());
            sliderModDefAway.setValue(match.modDefAway());

            chkMustWinHome.setSelected(match.isMustWinHome());
            chkMustWinAway.setSelected(match.isMustWinAway());
            chkLowUrgencyHome.setSelected(match.isLowUrgencyHome());
            chkLowUrgencyAway.setSelected(match.isLowUrgencyAway());
            chkNeutralVenue.setSelected(match.isNeutralVenue());

            // Pre-fill reference 1X2 back odds if present
            if (match.oddsHome() != null) txtBack1.setText(String.format("%.2f", match.oddsHome()));
            if (match.oddsDraw() != null) txtBackX.setText(String.format("%.2f", match.oddsDraw()));
            if (match.oddsAway() != null) txtBackAway(match.oddsAway());

            // Load saved market odds from repository
            loadPersistedMarketOdds(match.matchId());

        } finally {
            this.isUpdatingUi = false;
        }

        recalculateInference();
    }

    private void txtBackAway(Double oddsAway) {
        if (oddsAway != null) {
            txtBack2.setText(String.format("%.2f", oddsAway));
        }
    }

    private void loadPersistedMarketOdds(int matchId) {
        try {
            List<MarketOdds> savedOdds = manageMarketOddsUseCase.getOddsForMatch(matchId);
            for (MarketOdds odds : savedOdds) {
                if (odds.getMarketType() == MarketType.MATCH_ODDS) {
                    if ("1".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBack1, txtLay1, odds);
                    } else if ("X".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBackX, txtLayX, odds);
                    } else if ("2".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBack2, txtLay2, odds);
                    }
                } else if (odds.getMarketType() == MarketType.UNDER_OVER_25) {
                    if ("UNDER".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBackUnder25, txtLayUnder25, odds);
                    } else if ("OVER".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBackOver25, txtLayOver25, odds);
                    }
                } else if (odds.getMarketType() == MarketType.BTTS) {
                    if ("YES".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBackBttsYes, txtLayBttsYes, odds);
                    } else if ("NO".equalsIgnoreCase(odds.getOutcome())) {
                        setOddsFields(txtBackBttsNo, txtLayBttsNo, odds);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not load persisted market odds for match ID {}", matchId, e);
        }
    }

    private void setOddsFields(TextField backField, TextField layField, MarketOdds odds) {
        if (odds.getBackOdds() != null) backField.setText(String.format("%.2f", odds.getBackOdds()));
        if (odds.getLayOdds() != null) layField.setText(String.format("%.2f", odds.getLayOdds()));
    }

    private void triggerRecalculation() {
        if (!isUpdatingUi) {
            recalculateInference();
        }
    }

    // --- Core Quantitative Engine Recalculation ---

    private void recalculateInference() {
        if (currentMatch == null) return;

        try {
            // Base expectation rates
            double baseLambdaH = 1.45;
            double baseMuA = 1.15;

            // Apply tactical multiplier sliders
            double lambdaH = baseLambdaH * sliderModAttHome.getValue() * sliderModDefAway.getValue();
            double muA = baseMuA * sliderModAttAway.getValue() * sliderModDefHome.getValue();

            // Apply Home Advantage (if not neutral venue)
            if (!chkNeutralVenue.isSelected()) {
                lambdaH *= 1.15;
                muA *= (1.0 / 1.15);
            }

            // Apply Low-Urgency mutual risk aversion
            if (chkLowUrgencyHome.isSelected() && chkLowUrgencyAway.isSelected()) {
                lambdaH *= 0.65;
                muA *= 0.65;
            }

            lblLambdaHome.setText(String.format("%.2f", lambdaH));
            lblMuAway.setText(String.format("%.2f", muA));

            // Assemble market odds list from input fields
            List<MarketOdds> oddsList = assembleCurrentMarketOddsList();

            double commission = (currentSettings != null) ? currentSettings.getCommissionRate() : 0.05;

            // Execute Dixon-Coles and EV calculations
            PreMatchAnalysisResult result = calculatePreMatchInferenceUseCase.calculate(
                    lambdaH,
                    muA,
                    currentMatch.dixonColesRho(),
                    commission,
                    oddsList
            );

            // Update 1X2 Market UI
            updatePredictionUi(result.homeWin(), lblProb1, lblFairOdds1, lblEvBack1, lblEvLay1);
            updatePredictionUi(result.draw(), lblProbX, lblFairOddsX, lblEvBackX, lblEvLayX);
            updatePredictionUi(result.awayWin(), lblProb2, lblFairOdds2, lblEvBack2, lblEvLay2);

            // Update Under/Over 2.5 Market UI
            if (result.underOverPredictions().size() >= 6) {
                MarketPrediction u25 = result.underOverPredictions().get(4);
                MarketPrediction o25 = result.underOverPredictions().get(5);
                updatePredictionUi(u25, lblProbUnder25, lblFairOddsUnder25, lblEvBackUnder25, lblEvLayUnder25);
                updatePredictionUi(o25, lblProbOver25, lblFairOddsOver25, lblEvBackOver25, lblEvLayOver25);
            }

            // Update BTTS Market UI
            updatePredictionUi(result.bttsYes(), lblProbBttsYes, lblFairOddsBttsYes, lblEvBackBttsYes, lblEvLayBttsYes);
            updatePredictionUi(result.bttsNo(), lblProbBttsNo, lblFairOddsBttsNo, lblEvBackBttsNo, lblEvLayBttsNo);

        } catch (Exception e) {
            log.error("Error during pre-match quantitative inference", e);
            lblStatus.setText("Errore di calcolo: " + e.getMessage());
        }
    }

    private void updatePredictionUi(MarketPrediction pred, Label lblProb, Label lblFair, Label lblEvBack, Label lblEvLay) {
        if (pred == null) return;

        lblProb.setText(String.format("%.1f%%", pred.probability() * 100.0));
        lblFair.setText(String.format("%.2f", pred.fairOdds()));

        if (pred.evEvaluation() != null) {
            // EV Back
            if (pred.evEvaluation().evBack() != null) {
                double evBack = pred.evEvaluation().evBack();
                lblEvBack.setText(String.format("%+.1f%%", evBack * 100.0));
                applyEvStyling(lblEvBack, evBack);
            } else {
                lblEvBack.setText("-");
                lblEvBack.getStyleClass().removeAll("badge-ev-positive");
            }

            // EV Lay (Risk-Adjusted)
            if (pred.evEvaluation().evLayRiskAdjusted() != null) {
                double evLayRisk = pred.evEvaluation().evLayRiskAdjusted();
                lblEvLay.setText(String.format("%+.1f%%", evLayRisk * 100.0));
                applyEvStyling(lblEvLay, evLayRisk);
            } else {
                lblEvLay.setText("-");
                lblEvLay.getStyleClass().removeAll("badge-ev-positive");
            }
        } else {
            lblEvBack.setText("-");
            lblEvLay.setText("-");
            lblEvBack.getStyleClass().removeAll("badge-ev-positive");
            lblEvLay.getStyleClass().removeAll("badge-ev-positive");
        }
    }

    private void applyEvStyling(Label label, double ev) {
        label.getStyleClass().removeAll("badge", "badge-ev-positive");
        if (ev > 0.0) {
            label.getStyleClass().addAll("badge", "badge-ev-positive");
        }
    }

    private List<MarketOdds> assembleCurrentMarketOddsList() {
        if (currentMatch == null) return Collections.emptyList();
        int mId = currentMatch.matchId();
        List<MarketOdds> list = new ArrayList<>();

        addOddsIfPresent(list, mId, MarketType.MATCH_ODDS, "1", txtBack1, txtLay1);
        addOddsIfPresent(list, mId, MarketType.MATCH_ODDS, "X", txtBackX, txtLayX);
        addOddsIfPresent(list, mId, MarketType.MATCH_ODDS, "2", txtBack2, txtLay2);

        addOddsIfPresent(list, mId, MarketType.UNDER_OVER_25, "UNDER", txtBackUnder25, txtLayUnder25);
        addOddsIfPresent(list, mId, MarketType.UNDER_OVER_25, "OVER", txtBackOver25, txtLayOver25);

        addOddsIfPresent(list, mId, MarketType.BTTS, "YES", txtBackBttsYes, txtLayBttsYes);
        addOddsIfPresent(list, mId, MarketType.BTTS, "NO", txtBackBttsNo, txtLayBttsNo);

        return list;
    }

    private void addOddsIfPresent(List<MarketOdds> list, int matchId, MarketType type, String outcome, TextField backTf, TextField layTf) {
        Double back = parseDoubleOrNull(backTf.getText());
        Double lay = parseDoubleOrNull(layTf.getText());
        if (back != null || lay != null) {
            list.add(MarketOdds.create(matchId, type, outcome, back, lay));
        }
    }

    // --- Action & Event Handlers ---

    @FXML
    public void handleResetModifiers(ActionEvent event) {
        isUpdatingUi = true;
        try {
            sliderModAttHome.setValue(1.0);
            sliderModDefHome.setValue(1.0);
            sliderModAttAway.setValue(1.0);
            sliderModDefAway.setValue(1.0);

            chkMustWinHome.setSelected(false);
            chkMustWinAway.setSelected(false);
            chkLowUrgencyHome.setSelected(false);
            chkLowUrgencyAway.setSelected(false);
            chkNeutralVenue.setSelected(false);
        } finally {
            isUpdatingUi = false;
        }
        recalculateInference();
    }

    @FXML
    public void handleSaveMarketOdds(ActionEvent event) {
        if (currentMatch == null) return;

        try {
            int matchId = currentMatch.matchId();
            List<SaveMarketOddsCommand> commands = new ArrayList<>();

            addSaveCommand(commands, matchId, MarketType.MATCH_ODDS, "1", txtBack1, txtLay1);
            addSaveCommand(commands, matchId, MarketType.MATCH_ODDS, "X", txtBackX, txtLayX);
            addSaveCommand(commands, matchId, MarketType.MATCH_ODDS, "2", txtBack2, txtLay2);

            addSaveCommand(commands, matchId, MarketType.UNDER_OVER_25, "UNDER", txtBackUnder25, txtLayUnder25);
            addSaveCommand(commands, matchId, MarketType.UNDER_OVER_25, "OVER", txtBackOver25, txtLayOver25);

            addSaveCommand(commands, matchId, MarketType.BTTS, "YES", txtBackBttsYes, txtLayBttsYes);
            addSaveCommand(commands, matchId, MarketType.BTTS, "NO", txtBackBttsNo, txtLayBttsNo);

            manageMarketOddsUseCase.saveBatchOdds(commands);

            // Persist updated modifiers to match
            MatchModifiers modifiers = new MatchModifiers(
                    chkNeutralVenue.isSelected(),
                    chkMustWinHome.isSelected(),
                    chkMustWinAway.isSelected(),
                    chkLowUrgencyHome.isSelected(),
                    chkLowUrgencyAway.isSelected(),
                    sliderModAttHome.getValue(),
                    sliderModDefHome.getValue(),
                    sliderModAttAway.getValue(),
                    sliderModDefAway.getValue()
            );

            manageMatchUseCase.updateMatch(new UpdateMatchCommand(
                    matchId,
                    currentMatch.matchDateTime(),
                    parseDoubleOrNull(txtBack1.getText()),
                    parseDoubleOrNull(txtBackX.getText()),
                    parseDoubleOrNull(txtBack2.getText()),
                    modifiers
            ));

            lblStatus.setText("Quote exchange e modificatori salvati con successo nel database!");
        } catch (NepeException e) {
            lblStatus.setText("Errore durante il salvataggio: " + e.getMessage());
        }
    }

    private void addSaveCommand(List<SaveMarketOddsCommand> commands, int matchId, MarketType type, String outcome, TextField b, TextField l) {
        Double back = parseDoubleOrNull(b.getText());
        Double lay = parseDoubleOrNull(l.getText());
        if (back != null || lay != null) {
            commands.add(new SaveMarketOddsCommand(matchId, type, outcome, back, lay));
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

    // --- Utility Helpers ---

    private static Double parseDoubleOrNull(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return Double.parseDouble(str.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatDateTime(Instant instant) {
        return (instant != null) ? DATE_TIME_FORMATTER.format(instant) : "-";
    }
}
