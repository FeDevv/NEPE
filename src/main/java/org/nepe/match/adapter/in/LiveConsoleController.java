package org.nepe.match.adapter.in;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.inference.domain.EvCalculator;
import org.nepe.inference.domain.TeamStrengthCalculator;
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
import java.util.*;

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

    public static final String ENTRY_TYPE_LONG = "PUNTA (Long)";
    public static final String ENTRY_TYPE_SHORT = "BANCA (Short)";
    public static final String MSG_WAITING_BANCA_ODDS = "In attesa quota Banca...";
    public static final String MSG_WAITING_PUNTA_ODDS = "In attesa quota Punta...";

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

    // --- FXML Quick Odds Exchange & Live EV ---
    @FXML private Button btnClearLiveOdds;
    @FXML private Label lblQuickFairOdds1;
    @FXML private TextField txtLiveBack1;
    @FXML private TextField txtLiveLay1;
    @FXML private Label lblLiveEvBack1;
    @FXML private Label lblLiveEvLay1;

    @FXML private Label lblQuickFairOddsX;
    @FXML private TextField txtLiveBackX;
    @FXML private TextField txtLiveLayX;
    @FXML private Label lblLiveEvBackX;
    @FXML private Label lblLiveEvLayX;

    @FXML private Label lblQuickFairOdds2;
    @FXML private TextField txtLiveBack2;
    @FXML private TextField txtLiveLay2;
    @FXML private Label lblLiveEvBack2;
    @FXML private Label lblLiveEvLay2;

    @FXML private ComboBox<String> comboLiveUoLine;
    @FXML private Label lblQuickTitleUnder;
    @FXML private Label lblQuickFairOddsUnder;
    @FXML private TextField txtLiveBackUnder;
    @FXML private TextField txtLiveLayUnder;
    @FXML private Label lblLiveEvBackUnder;
    @FXML private Label lblLiveEvLayUnder;

    @FXML private Label lblQuickTitleOver;
    @FXML private Label lblQuickFairOddsOver;
    @FXML private TextField txtLiveBackOver;
    @FXML private TextField txtLiveLayOver;
    @FXML private Label lblLiveEvBackOver;
    @FXML private Label lblLiveEvLayOver;

    @FXML private ComboBox<String> comboEntryType;
    @FXML private ComboBox<String> comboEntryOutcome;
    @FXML private TextField txtEntryOdds;
    @FXML private Label lblLiveGreenUpProfit;

    // --- FXML Residual Probability Grid: 1X2 & BTTS ---
    @FXML private Label lblResidualRates;
    @FXML private Label lblLiveProb1;
    @FXML private Label lblLiveProbX;
    @FXML private Label lblLiveProb2;
    @FXML private Label lblLiveProbBtts;

    @FXML private Label lblLiveFairOdds1;
    @FXML private Label lblLiveFairOddsX;
    @FXML private Label lblLiveFairOdds2;
    @FXML private Label lblLiveFairOddsBtts;

    // --- FXML Residual Probability Grid: Under/Over 0.5 - 4.5 ---
    @FXML private Label lblLiveProbUnder05;
    @FXML private Label lblLiveFairOddsUnder05;
    @FXML private Label lblLiveProbOver05;
    @FXML private Label lblLiveFairOddsOver05;

    @FXML private Label lblLiveProbUnder15;
    @FXML private Label lblLiveFairOddsUnder15;
    @FXML private Label lblLiveProbOver15;
    @FXML private Label lblLiveFairOddsOver15;

    @FXML private Label lblLiveProbUnder25;
    @FXML private Label lblLiveFairOddsUnder25;
    @FXML private Label lblLiveProbOver25;
    @FXML private Label lblLiveFairOddsOver25;

    @FXML private Label lblLiveProbUnder35;
    @FXML private Label lblLiveFairOddsUnder35;
    @FXML private Label lblLiveProbOver35;
    @FXML private Label lblLiveFairOddsOver35;

    @FXML private Label lblLiveProbUnder45;
    @FXML private Label lblLiveFairOddsUnder45;
    @FXML private Label lblLiveProbOver45;
    @FXML private Label lblLiveFairOddsOver45;

    // --- FXML Events Chronology Log & Status ---
    @FXML private ListView<String> lstEventsLog;
    @FXML private Label lblStatus;

    // --- Live State ---
    private MatchDetailsDTO currentMatch;
    private Integer scopeCompetitionId;
    private Integer scopeSeasonId;
    private int currentHomeScore = 0;
    private int currentAwayScore = 0;
    private int currentHomeRedCards = 0;
    private int currentAwayRedCards = 0;
    private int currentMinute = 0;
    private AppSettings currentSettings;
    private boolean isUpdatingQuickOdds = false;

    // --- Cached Pre-Match Calculation Parameters (Avoiding redundant synchronous DB I/O on UI thread) ---
    private double cachedLambdaPre = 0.0;
    private double cachedMuPre = 0.0;
    private MatchModifiers cachedModifiers;
    private List<MarketOdds> cachedStoredOdds = Collections.emptyList();

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
        configureQuickOddsInputs();
        loadAvailableLiveMatches();
        updateControlStates();
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
            if (newMatch != null && (currentMatch == null || currentMatch.matchId() != newMatch.matchId())) {
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

    private void configureQuickOddsInputs() {
        if (comboLiveUoLine != null) {
            comboLiveUoLine.setItems(FXCollections.observableArrayList("0.5", "1.5", "2.5", "3.5", "4.5"));
            comboLiveUoLine.setValue("2.5");
            comboLiveUoLine.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                String line = (newVal != null) ? newVal : "2.5";
                if (lblQuickTitleUnder != null) lblQuickTitleUnder.setText("Under " + line);
                if (lblQuickTitleOver != null) lblQuickTitleOver.setText("Over " + line);
                if (!isUpdatingQuickOdds) {
                    recalculateLiveInference();
                }
            });
        }

        if (comboEntryType != null) {
            comboEntryType.setItems(FXCollections.observableArrayList(ENTRY_TYPE_LONG, ENTRY_TYPE_SHORT));
            comboEntryType.setValue(ENTRY_TYPE_LONG);
            comboEntryType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingQuickOdds) {
                    recalculateLiveInference();
                }
            });
        }

        if (comboEntryOutcome != null) {
            comboEntryOutcome.setItems(FXCollections.observableArrayList("1", "X", "2", "UNDER", "OVER"));
            comboEntryOutcome.setValue("1");
            comboEntryOutcome.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingQuickOdds) {
                    recalculateLiveInference();
                }
            });
        }

        attachOddsFieldListener(txtLiveBack1);
        attachOddsFieldListener(txtLiveLay1);
        attachOddsFieldListener(txtLiveBackX);
        attachOddsFieldListener(txtLiveLayX);
        attachOddsFieldListener(txtLiveBack2);
        attachOddsFieldListener(txtLiveLay2);
        attachOddsFieldListener(txtLiveBackUnder);
        attachOddsFieldListener(txtLiveLayUnder);
        attachOddsFieldListener(txtLiveBackOver);
        attachOddsFieldListener(txtLiveLayOver);
        attachOddsFieldListener(txtEntryOdds);
    }

    private void attachOddsFieldListener(TextField tf) {
        if (tf != null) {
            tf.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingQuickOdds) {
                    recalculateLiveInference();
                }
            });
        }
    }

    private void loadAvailableLiveMatches() {
        try {
            List<MatchDetailsDTO> allMatches = manageMatchUseCase.getAllMatchDetails();
            List<MatchDetailsDTO> matches = allMatches.stream()
                    .filter(m -> m.matchState() == MatchState.LIVE || m.matchState() == MatchState.SCHEDULED)
                    .sorted(java.util.Comparator.comparing((MatchDetailsDTO m) -> m.matchState() == MatchState.LIVE ? 0 : 1)
                            .thenComparing(MatchDetailsDTO::matchDateTime))
                    .toList();

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
        this.scopeCompetitionId = competitionId;
        this.scopeSeasonId = seasonId;
        try {
            List<MatchDetailsDTO> liveMatches = manageMatchUseCase.getMatchDetailsByState(competitionId, seasonId, MatchState.LIVE);
            List<MatchDetailsDTO> scheduledMatches = manageMatchUseCase.getMatchDetailsByState(competitionId, seasonId, MatchState.SCHEDULED);
            List<MatchDetailsDTO> combined = new java.util.ArrayList<>(liveMatches);
            combined.addAll(scheduledMatches);
            if (comboLiveMatchSelector != null) {
                comboLiveMatchSelector.setItems(FXCollections.observableArrayList(combined));
                if (!combined.isEmpty()) {
                    comboLiveMatchSelector.getSelectionModel().selectFirst();
                }
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
        this.scopeCompetitionId = match.competitionId();
        this.scopeSeasonId = match.seasonId();
        this.currentSettings = manageSettingsUseCase.getSettings();

        // 1. Pre-compute and cache pre-match parameters immediately
        computeAndCachePreMatchParameters(match);

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

        // Pre-populate entry position with pre-match oddsHome if available and reset volatile live inputs
        isUpdatingQuickOdds = true;
        try {
            clearAllQuickOddsFields();
            if (txtEntryOdds != null && match.oddsHome() != null) {
                txtEntryOdds.setText(String.format(Locale.US, "%.2f", match.oddsHome()));
            }
            if (comboEntryType != null) {
                comboEntryType.setValue(ENTRY_TYPE_LONG);
            }
            if (comboEntryOutcome != null) {
                comboEntryOutcome.setValue("1");
            }
        } finally {
            isUpdatingQuickOdds = false;
        }

        reloadEventsHistory();
        recalculateLiveInference();
        updateControlStates();
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

    /**
     * Pre-computes and caches pre-match rates (lambdaPre, muPre), match modifiers,
     * and stored baseline odds upon match selection or match configuration changes.
     * <p>
     * This isolates heavy synchronous database queries and mathematical team strength aggregations
     * from high-frequency in-game events (minute stepper, live odds typing, score changes),
     * ensuring zero-lag 60 FPS responsiveness on the JavaFX Application Thread.
     *
     * @param match match details DTO
     */
    private void computeAndCachePreMatchParameters(MatchDetailsDTO match) {
        if (match == null) return;

        try {
            int defaultN = (currentSettings != null) ? currentSettings.getDefaultNMatches() : 10;
            double gamma = (currentSettings != null) ? currentSettings.getSeasonalDecayGamma() : 0.70;

            // 1. Fetch team historical match performances (N_min = 10 with previous season gamma decay)
            List<TeamStrengthCalculator.MatchPerformance> homeHistory =
                    manageMatchUseCase.getHistoricalTeamPerformances(match.homeTeamId(), match.competitionId(), match.seasonId(), defaultN);
            List<TeamStrengthCalculator.MatchPerformance> awayHistory =
                    manageMatchUseCase.getHistoricalTeamPerformances(match.awayTeamId(), match.competitionId(), match.seasonId(), defaultN);

            double leagueAvgXg = manageMatchUseCase.getLeagueAverageXgPerTeam(match.competitionId(), match.seasonId());

            TeamStrengthCalculator.TeamStrength homeStrength =
                    TeamStrengthCalculator.calculateStrength(homeHistory, leagueAvgXg, gamma);
            TeamStrengthCalculator.TeamStrength awayStrength =
                    TeamStrengthCalculator.calculateStrength(awayHistory, leagueAvgXg, gamma);

            double baseHomeAdv = manageMatchUseCase.getDynamicHomeAdvantage(match.competitionId(), match.seasonId());
            double homeAdv = match.isNeutralVenue() ? 1.0 : baseHomeAdv;

            this.cachedModifiers = new MatchModifiers(
                    match.isNeutralVenue(),
                    match.isMustWinHome(),
                    match.isMustWinAway(),
                    match.isLowUrgencyHome(),
                    match.isLowUrgencyAway(),
                    match.modAttHome(),
                    match.modDefHome(),
                    match.modAttAway(),
                    match.modDefAway()
            );

            TeamStrengthCalculator.PreMatchRates preRates =
                    TeamStrengthCalculator.calculatePreMatchRates(
                            homeStrength, awayStrength, leagueAvgXg, homeAdv, this.cachedModifiers
                    );

            this.cachedLambdaPre = preRates.lambdaHome();
            this.cachedMuPre = preRates.muAway();

            // Cache stored baseline market odds for this match
            List<MarketOdds> storedOdds = manageMarketOddsUseCase.getOddsForMatch(match.matchId());
            this.cachedStoredOdds = (storedOdds != null) ? storedOdds : Collections.emptyList();
        } catch (Exception e) {
            log.error("Could not compute pre-match parameters for match ID {}", match.matchId(), e);
        }
    }

    // --- Core Real-Time In-Game Probability Recalculation ---

    private void recalculateLiveInference() {
        if (currentMatch == null) return;

        try {
            if (cachedModifiers == null) {
                computeAndCachePreMatchParameters(currentMatch);
            }

            double commission = (currentSettings != null) ? currentSettings.getCommissionRate() : 0.05;
            double profitTarget = (currentSettings != null) ? currentSettings.getGreenUpProfitTarget() : 0.10;

            // 1. Assemble market odds: preload cached baseline odds and overlay with in-memory quick live odds
            Map<String, MarketOdds> liveOddsMap = new HashMap<>();
            if (cachedStoredOdds != null) {
                for (MarketOdds o : cachedStoredOdds) {
                    if (o != null && o.getMarketType() != null && o.getOutcome() != null) {
                        liveOddsMap.put(o.getMarketType().name() + ":" + o.getOutcome().trim().toUpperCase(), o);
                    }
                }
            }

            // Overlay in-memory quick live odds (1X2)
            addQuickOddsIfPresent(liveOddsMap, currentMatch.matchId(), MarketType.MATCH_ODDS, "1", txtLiveBack1, txtLiveLay1);
            addQuickOddsIfPresent(liveOddsMap, currentMatch.matchId(), MarketType.MATCH_ODDS, "X", txtLiveBackX, txtLiveLayX);
            addQuickOddsIfPresent(liveOddsMap, currentMatch.matchId(), MarketType.MATCH_ODDS, "2", txtLiveBack2, txtLiveLay2);

            // Overlay in-memory quick live odds (active Under/Over line)
            String activeLine = (comboLiveUoLine != null && comboLiveUoLine.getValue() != null) ? comboLiveUoLine.getValue() : "2.5";
            MarketType activeUoMarket = getMarketTypeForLine(activeLine);
            addQuickOddsIfPresent(liveOddsMap, currentMatch.matchId(), activeUoMarket, "UNDER", txtLiveBackUnder, txtLiveLayUnder);
            addQuickOddsIfPresent(liveOddsMap, currentMatch.matchId(), activeUoMarket, "OVER", txtLiveBackOver, txtLiveLayOver);

            List<MarketOdds> liveOddsList = new ArrayList<>(liveOddsMap.values());

            // 2. Resolve entry position parameters for Green-Up evaluation
            String selectedOutcome = (comboEntryOutcome != null && comboEntryOutcome.getValue() != null)
                    ? comboEntryOutcome.getValue() : "1";
            MarketType entryMarket;
            String entryOutcomeKey;
            if ("UNDER".equalsIgnoreCase(selectedOutcome) || "OVER".equalsIgnoreCase(selectedOutcome)) {
                entryMarket = activeUoMarket;
                entryOutcomeKey = selectedOutcome.toUpperCase();
            } else {
                entryMarket = MarketType.MATCH_ODDS;
                entryOutcomeKey = selectedOutcome;
            }

            Double entryOdds = parseSafeDouble(txtEntryOdds);
            boolean isShort = (comboEntryType != null && ENTRY_TYPE_SHORT.equals(comboEntryType.getValue()));
            if (entryOdds == null && !isShort && "1".equals(entryOutcomeKey) && currentMatch.oddsHome() != null) {
                entryOdds = currentMatch.oddsHome();
            }

            LiveInferenceQuery query = new LiveInferenceQuery(
                    cachedLambdaPre,
                    cachedMuPre,
                    currentMinute,
                    currentHomeScore,
                    currentAwayScore,
                    currentHomeRedCards,
                    currentAwayRedCards,
                    cachedModifiers,
                    currentMatch.dixonColesRho(),
                    commission,
                    profitTarget,
                    liveOddsList,
                    entryOdds,
                    entryMarket,
                    entryOutcomeKey
            );

            LiveAnalysisResult result = calculateLiveInferenceUseCase.calculate(query);

            // Update residual rates display
            lblResidualRates.setText(String.format("λ residuo: %.2f | μ residuo: %.2f (Minuto: %d')",
                    result.lambdaHomeResidual(), result.muAwayResidual(), result.currentMinute()));

            // Update Probabilities and Fair Odds (Reference Grid)
            updateLivePrediction(result.finalHomeWin(), lblLiveProb1, lblLiveFairOdds1);
            updateLivePrediction(result.finalDraw(), lblLiveProbX, lblLiveFairOddsX);
            updateLivePrediction(result.finalAwayWin(), lblLiveProb2, lblLiveFairOdds2);

            if (result.underOverPredictions().size() >= 10) {
                updateLivePrediction(result.underOverPredictions().get(0), lblLiveProbUnder05, lblLiveFairOddsUnder05);
                updateLivePrediction(result.underOverPredictions().get(1), lblLiveProbOver05, lblLiveFairOddsOver05);
                updateLivePrediction(result.underOverPredictions().get(2), lblLiveProbUnder15, lblLiveFairOddsUnder15);
                updateLivePrediction(result.underOverPredictions().get(3), lblLiveProbOver15, lblLiveFairOddsOver15);
                updateLivePrediction(result.underOverPredictions().get(4), lblLiveProbUnder25, lblLiveFairOddsUnder25);
                updateLivePrediction(result.underOverPredictions().get(5), lblLiveProbOver25, lblLiveFairOddsOver25);
                updateLivePrediction(result.underOverPredictions().get(6), lblLiveProbUnder35, lblLiveFairOddsUnder35);
                updateLivePrediction(result.underOverPredictions().get(7), lblLiveProbOver35, lblLiveFairOddsOver35);
                updateLivePrediction(result.underOverPredictions().get(8), lblLiveProbUnder45, lblLiveFairOddsUnder45);
                updateLivePrediction(result.underOverPredictions().get(9), lblLiveProbOver45, lblLiveFairOddsOver45);
            }

            updateLivePrediction(result.bttsYes(), lblLiveProbBtts, lblLiveFairOddsBtts);

            // Update Quick Odds Exchange UI (EV Back & EV Lay Risk-Adjusted)
            updateQuickOddsDisplay(result, activeLine);

            // Update Green-Up display and banner
            updateGreenUpDisplay(result, entryOdds, entryMarket, entryOutcomeKey, liveOddsMap, profitTarget);

            lblStatus.setText("");
        } catch (Exception e) {
            log.error("Error calculating live probability inferences", e);
            lblStatus.setText("Errore calcolo probabilità live: " + e.getMessage());
        }
    }

    private void updateLivePrediction(MarketPrediction pred, Label lblProb, Label lblFair) {
        if (pred == null) return;
        if (lblProb != null) lblProb.setText(String.format("%.1f%%", pred.probability() * 100.0));
        if (lblFair != null) lblFair.setText(String.format("Quota: %.2f", pred.fairOdds()));
    }

    private void updateQuickOddsDisplay(LiveAnalysisResult result, String activeLine) {
        if (result == null) return;

        // 1X2 Quick Outcomes
        updateQuickOutcomeUi(result.finalHomeWin(), txtLiveBack1, txtLiveLay1, lblQuickFairOdds1, lblLiveEvBack1, lblLiveEvLay1);
        updateQuickOutcomeUi(result.finalDraw(), txtLiveBackX, txtLiveLayX, lblQuickFairOddsX, lblLiveEvBackX, lblLiveEvLayX);
        updateQuickOutcomeUi(result.finalAwayWin(), txtLiveBack2, txtLiveLay2, lblQuickFairOdds2, lblLiveEvBack2, lblLiveEvLay2);

        // Active Under / Over Line
        int uoIndex = getUnderIndexForLine(activeLine);
        if (result.underOverPredictions().size() > uoIndex + 1) {
            MarketPrediction underPred = result.underOverPredictions().get(uoIndex);
            MarketPrediction overPred = result.underOverPredictions().get(uoIndex + 1);
            updateQuickOutcomeUi(underPred, txtLiveBackUnder, txtLiveLayUnder, lblQuickFairOddsUnder, lblLiveEvBackUnder, lblLiveEvLayUnder);
            updateQuickOutcomeUi(overPred, txtLiveBackOver, txtLiveLayOver, lblQuickFairOddsOver, lblLiveEvBackOver, lblLiveEvLayOver);
        }
    }

    private void updateQuickOutcomeUi(MarketPrediction pred, TextField txtBack, TextField txtLay,
                                      Label lblFair, Label lblEvBack, Label lblEvLay) {
        if (pred == null) return;
        if (lblFair != null) {
            lblFair.setText(String.format(Locale.US, "Equa: %.2f", pred.fairOdds()));
        }

        Double enteredBack = parseSafeDouble(txtBack);
        if (enteredBack != null && pred.evEvaluation() != null && pred.evEvaluation().evBack() != null) {
            double evBack = pred.evEvaluation().evBack();
            if (lblEvBack != null) {
                lblEvBack.setText(String.format(Locale.US, "%+.1f%%", evBack * 100.0));
                applyEvStyling(lblEvBack, evBack);
            }
        } else if (lblEvBack != null) {
            lblEvBack.setText("-");
            lblEvBack.getStyleClass().removeAll("badge", "badge-ev-positive");
        }

        Double enteredLay = parseSafeDouble(txtLay);
        if (enteredLay != null && pred.evEvaluation() != null && pred.evEvaluation().evLayRiskAdjusted() != null) {
            double evLay = pred.evEvaluation().evLayRiskAdjusted();
            if (lblEvLay != null) {
                lblEvLay.setText(String.format(Locale.US, "%+.1f%%", evLay * 100.0));
                applyEvStyling(lblEvLay, evLay);
            }
        } else if (lblEvLay != null) {
            lblEvLay.setText("-");
            lblEvLay.getStyleClass().removeAll("badge", "badge-ev-positive");
        }
    }

    private void updateGreenUpDisplay(LiveAnalysisResult result, Double entryOdds, MarketType entryMarket,
                                      String entryOutcome, Map<String, MarketOdds> liveOddsMap, double profitTarget) {
        boolean isShort = (comboEntryType != null && ENTRY_TYPE_SHORT.equals(comboEntryType.getValue()));

        boolean greenUpActive = false;
        double profitRatio = 0.0;
        boolean hasValidOdds = false;

        if (entryOdds != null && entryOdds > 1.0) {
            String key = entryMarket.name() + ":" + entryOutcome.trim().toUpperCase();
            MarketOdds matching = liveOddsMap.get(key);

            if (isShort) {
                // Short position (Lay entry): liquidated by buying back at current Back odds
                if (matching != null && matching.getBackOdds() != null && matching.getBackOdds() > 1.0) {
                    double currentBack = matching.getBackOdds();
                    profitRatio = EvCalculator.calculateGreenUpProfitRatioLay(entryOdds, currentBack);
                    hasValidOdds = true;
                    greenUpActive = (profitRatio >= profitTarget);
                } else if (lblLiveGreenUpProfit != null) {
                    lblLiveGreenUpProfit.setText(MSG_WAITING_PUNTA_ODDS);
                    lblLiveGreenUpProfit.setStyle("-fx-text-fill: #9ca3af;");
                }
            } else {
                // Long position (Back entry): liquidated by laying at current Lay odds
                if (matching != null && matching.getLayOdds() != null && matching.getLayOdds() > 1.0) {
                    double currentLay = matching.getLayOdds();
                    profitRatio = EvCalculator.calculateGreenUpProfitRatioBack(entryOdds, currentLay);
                    hasValidOdds = true;
                    greenUpActive = (profitRatio >= profitTarget);
                } else if (lblLiveGreenUpProfit != null) {
                    lblLiveGreenUpProfit.setText(MSG_WAITING_BANCA_ODDS);
                    lblLiveGreenUpProfit.setStyle("-fx-text-fill: #9ca3af;");
                }
            }

            if (hasValidOdds) {
                if (lblLiveGreenUpProfit != null) {
                    lblLiveGreenUpProfit.setText(String.format(Locale.US, "%+.1f%% (Target: %.0f%%)",
                            profitRatio * 100.0, profitTarget * 100.0));
                    if (profitRatio >= profitTarget) {
                        lblLiveGreenUpProfit.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
                    } else if (profitRatio >= 0.0) {
                        lblLiveGreenUpProfit.setStyle("-fx-text-fill: #93c5fd; -fx-font-weight: bold;");
                    } else {
                        lblLiveGreenUpProfit.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                    }
                }
                if (greenUpActive && lblGreenUpText != null) {
                    String direction = isShort ? "Banca → Punta" : "Punta → Banca";
                    lblGreenUpText.setText(String.format(Locale.US,
                            "Target di Green Up (Cash Out %s) Raggiunto! Profitto stimato: %+.1f%% (Target: %.0f%%). Chiudi la posizione per bloccare il profitto.",
                            direction, profitRatio * 100.0, profitTarget * 100.0));
                }
            }
        } else if (lblLiveGreenUpProfit != null) {
            lblLiveGreenUpProfit.setText("N/A");
            lblLiveGreenUpProfit.setStyle("-fx-text-fill: #9ca3af;");
        }

        if (boxGreenUpBanner != null) {
            boxGreenUpBanner.setVisible(greenUpActive);
            boxGreenUpBanner.setManaged(greenUpActive);
        }
    }

    private void addQuickOddsIfPresent(Map<String, MarketOdds> map, int matchId, MarketType type, String outcome,
                                       TextField txtBack, TextField txtLay) {
        Double back = parseSafeDouble(txtBack);
        Double lay = parseSafeDouble(txtLay);
        if (back != null || lay != null) {
            String key = type.name() + ":" + outcome.trim().toUpperCase();
            map.put(key, MarketOdds.create(matchId, type, outcome, back, lay));
        }
    }

    private Double parseSafeDouble(TextField tf) {
        if (tf == null || tf.getText() == null || tf.getText().isBlank()) {
            return null;
        }
        try {
            double val = Double.parseDouble(tf.getText().trim().replace(',', '.'));
            return (val > 1.0) ? val : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private MarketType getMarketTypeForLine(String line) {
        if (line == null) return MarketType.UNDER_OVER_25;
        return switch (line.trim()) {
            case "0.5" -> MarketType.UNDER_OVER_05;
            case "1.5" -> MarketType.UNDER_OVER_15;
            case "2.5" -> MarketType.UNDER_OVER_25;
            case "3.5" -> MarketType.UNDER_OVER_35;
            case "4.5" -> MarketType.UNDER_OVER_45;
            default -> MarketType.UNDER_OVER_25;
        };
    }

    private int getUnderIndexForLine(String line) {
        if (line == null) return 4;
        return switch (line.trim()) {
            case "0.5" -> 0;
            case "1.5" -> 2;
            case "2.5" -> 4;
            case "3.5" -> 6;
            case "4.5" -> 8;
            default -> 4;
        };
    }

    private void applyEvStyling(Label label, double ev) {
        label.getStyleClass().removeAll("badge", "badge-ev-positive");
        if (ev > 0.0) {
            label.getStyleClass().addAll("badge", "badge-ev-positive");
        }
    }

    @FXML
    public void handleClearLiveOdds(ActionEvent event) {
        clearAllQuickOddsFields();
        recalculateLiveInference();
        if (lblStatus != null) {
            lblStatus.setText("Quote live in memoria azzerate.");
        }
    }

    private void clearAllQuickOddsFields() {
        boolean wasUpdating = isUpdatingQuickOdds;
        isUpdatingQuickOdds = true;
        try {
            if (txtLiveBack1 != null) txtLiveBack1.clear();
            if (txtLiveLay1 != null) txtLiveLay1.clear();
            if (txtLiveBackX != null) txtLiveBackX.clear();
            if (txtLiveLayX != null) txtLiveLayX.clear();
            if (txtLiveBack2 != null) txtLiveBack2.clear();
            if (txtLiveLay2 != null) txtLiveLay2.clear();
            if (txtLiveBackUnder != null) txtLiveBackUnder.clear();
            if (txtLiveLayUnder != null) txtLiveLayUnder.clear();
            if (txtLiveBackOver != null) txtLiveBackOver.clear();
            if (txtLiveLayOver != null) txtLiveLayOver.clear();
        } finally {
            isUpdatingQuickOdds = wasUpdating;
        }
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
        if (currentMatch == null || currentMatch.matchState() != MatchState.LIVE) return;

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
            MatchDetailsDTO updated = manageMatchUseCase.getMatchDetailsById(currentMatch.matchId());
            loadMatchDetails(updated);
            refreshMatchDropdownSelection(updated);
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
            MatchDetailsDTO updated = manageMatchUseCase.getMatchDetailsById(currentMatch.matchId());
            loadMatchDetails(updated);
            refreshMatchDropdownSelection(updated);
            lblStatus.setText("Partita conclusa e salvata.");
        } catch (Exception e) {
            lblStatus.setText("Errore conclusione partita: " + e.getMessage());
        }
    }

    private void updateControlStates() {
        if (currentMatch == null) {
            this.cachedLambdaPre = 0.0;
            this.cachedMuPre = 0.0;
            this.cachedModifiers = null;
            this.cachedStoredOdds = Collections.emptyList();
            if (btnStartLive != null) btnStartLive.setDisable(true);
            if (btnFinishMatch != null) btnFinishMatch.setDisable(true);
            setEventButtonsDisable(true);
            setMinuteControlsDisable(true);
            setQuickOddsControlsDisable(true);
            return;
        }

        MatchState state = currentMatch.matchState();
        boolean isLive = state == MatchState.LIVE;
        boolean isScheduled = state == MatchState.SCHEDULED;

        if (btnStartLive != null) btnStartLive.setDisable(!isScheduled);
        if (btnFinishMatch != null) btnFinishMatch.setDisable(!isLive);
        setEventButtonsDisable(!isLive);
        setMinuteControlsDisable(!isLive);
        setQuickOddsControlsDisable(!isLive && !isScheduled);
    }

    private void setQuickOddsControlsDisable(boolean disable) {
        if (btnClearLiveOdds != null) btnClearLiveOdds.setDisable(disable);
        if (txtLiveBack1 != null) txtLiveBack1.setDisable(disable);
        if (txtLiveLay1 != null) txtLiveLay1.setDisable(disable);
        if (txtLiveBackX != null) txtLiveBackX.setDisable(disable);
        if (txtLiveLayX != null) txtLiveLayX.setDisable(disable);
        if (txtLiveBack2 != null) txtLiveBack2.setDisable(disable);
        if (txtLiveLay2 != null) txtLiveLay2.setDisable(disable);
        if (comboLiveUoLine != null) comboLiveUoLine.setDisable(disable);
        if (txtLiveBackUnder != null) txtLiveBackUnder.setDisable(disable);
        if (txtLiveLayUnder != null) txtLiveLayUnder.setDisable(disable);
        if (txtLiveBackOver != null) txtLiveBackOver.setDisable(disable);
        if (txtLiveLayOver != null) txtLiveLayOver.setDisable(disable);
        if (comboEntryType != null) comboEntryType.setDisable(disable);
        if (comboEntryOutcome != null) comboEntryOutcome.setDisable(disable);
        if (txtEntryOdds != null) txtEntryOdds.setDisable(disable);
    }

    private void setEventButtonsDisable(boolean disable) {
        if (btnGoalHome != null) btnGoalHome.setDisable(disable);
        if (btnGoalAway != null) btnGoalAway.setDisable(disable);
        if (btnRedCardHome != null) btnRedCardHome.setDisable(disable);
        if (btnRedCardAway != null) btnRedCardAway.setDisable(disable);
        if (btnUndoLastEvent != null) btnUndoLastEvent.setDisable(disable);
    }

    private void setMinuteControlsDisable(boolean disable) {
        if (btnMinus1Min != null) btnMinus1Min.setDisable(disable);
        if (txtCurrentMinute != null) txtCurrentMinute.setDisable(disable);
        if (btnPlus1Min != null) btnPlus1Min.setDisable(disable);
        if (btnPlus5Min != null) btnPlus5Min.setDisable(disable);
    }

    private void refreshMatchDropdownSelection(MatchDetailsDTO updatedMatch) {
        if (comboLiveMatchSelector == null || updatedMatch == null) return;
        ObservableList<MatchDetailsDTO> items = comboLiveMatchSelector.getItems();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).matchId() == updatedMatch.matchId()) {
                    items.set(i, updatedMatch);
                    comboLiveMatchSelector.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        try {
            SpringFXMLLoader.ViewResult<Parent, DashboardController> view =
                    springFXMLLoader.loadWithController("/views/dashboard.fxml");
            if (scopeCompetitionId != null || scopeSeasonId != null) {
                view.controller().selectCompetitionAndSeason(scopeCompetitionId, scopeSeasonId);
            }
            Stage stage = (Stage) btnBackToDashboard.getScene().getWindow();
            stage.getScene().setRoot(view.rootNode());
            stage.setTitle("NEPE - Nexus Exchange Prediction Engine");
        } catch (Exception e) {
            log.error("Failed to return to dashboard", e);
        }
    }

    public Integer getScopeCompetitionId() {
        return scopeCompetitionId;
    }

    public Integer getScopeSeasonId() {
        return scopeSeasonId;
    }

    public double getCachedLambdaPre() {
        return cachedLambdaPre;
    }

    public double getCachedMuPre() {
        return cachedMuPre;
    }

    public MatchModifiers getCachedModifiers() {
        return cachedModifiers;
    }

    private static String formatDateTime(Instant instant) {
        return (instant != null) ? DATE_TIME_FORMATTER.format(instant) : "-";
    }
}
