package org.nepe.match.adapter.in;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.inference.domain.TeamStrengthCalculator;
import org.nepe.inference.port.in.CalculateLiveInferenceUseCase;
import org.nepe.inference.service.LiveInferenceService;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.LiveMatchTradingUseCase;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.ManageSettingsUseCase;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("LiveConsoleController Unit Tests")
class LiveConsoleControllerTest {

    private final LiveMatchTradingUseCase liveMatchTradingUseCase = mock(LiveMatchTradingUseCase.class);
    private final CalculateLiveInferenceUseCase calculateLiveInferenceUseCase = new LiveInferenceService();
    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final ManageMarketOddsUseCase manageMarketOddsUseCase = mock(ManageMarketOddsUseCase.class);
    private final ManageSettingsUseCase manageSettingsUseCase = mock(ManageSettingsUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit already initialized
        }
    }

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new LiveConsoleController(
                null, calculateLiveInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("LiveMatchTradingUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, null, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CalculateLiveInferenceUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, null, manageMarketOddsUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, manageMatchUseCase, null, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMarketOddsUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSettingsUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("LiveConsoleController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        LiveConsoleController controller = createController();

        assertThat(controller).isNotNull();
        assertThat(controller.getScopeCompetitionId()).isNull();
        assertThat(controller.getScopeSeasonId()).isNull();
    }

    @Test
    @DisplayName("setScope should store competition and season IDs and tolerate uninitialized UI controls safely")
    void shouldStoreScopeAndHandleUninitializedControlsSafely() {
        LiveConsoleController controller = createController();

        controller.setScope(10, 20);

        assertThat(controller.getScopeCompetitionId()).isEqualTo(10);
        assertThat(controller.getScopeSeasonId()).isEqualTo(20);
    }

    @Test
    @DisplayName("loadMatchDetails should safely ignore null match parameter without throwing NPE")
    void shouldSafelyHandleNullMatchInLoadMatchDetails() {
        LiveConsoleController controller = createController();

        controller.loadMatchDetails(null);

        assertThat(controller.getScopeCompetitionId()).isNull();
        assertThat(controller.getScopeSeasonId()).isNull();
    }

    @Test
    @DisplayName("Live event and state transition actions should safely handle null currentMatch without interacting with use cases")
    void shouldSafelyHandleNullMatchInActions() {
        LiveConsoleController controller = createController();

        controller.handleStartLive(null);
        controller.handleFinishMatch(null);
        controller.handleUndoLastEvent(null);
        controller.handleGoalHome(null);
        controller.handleGoalAway(null);
        controller.handleRedCardHome(null);
        controller.handleRedCardAway(null);

        verifyNoInteractions(liveMatchTradingUseCase);
        verifyNoInteractions(manageMatchUseCase);
    }

    @Test
    @DisplayName("initialize should configure quick odds lines, entry types, and initial defaults")
    void shouldInitializeQuickOddsAndEntryControlsCorrectly() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();

        ComboBox<String> comboUoLine = getField(controller, "comboLiveUoLine");
        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        ComboBox<String> comboEntryOutcome = getField(controller, "comboEntryOutcome");

        assertThat(comboUoLine.getItems()).containsExactly("0.5", "1.5", "2.5", "3.5", "4.5");
        assertThat(comboUoLine.getValue()).isEqualTo("2.5");

        assertThat(comboEntryType.getItems()).containsExactly(LiveConsoleController.ENTRY_TYPE_LONG, LiveConsoleController.ENTRY_TYPE_SHORT);
        assertThat(comboEntryType.getValue()).isEqualTo(LiveConsoleController.ENTRY_TYPE_LONG);

        assertThat(comboEntryOutcome.getItems()).containsExactly("1", "X", "2", "UNDER", "OVER");
        assertThat(comboEntryOutcome.getValue()).isEqualTo("1");
    }

    @Test
    @DisplayName("Typing live odds should reactively compute EV Back and EV Lay with proper badge styling")
    void shouldRecalculateLiveOddsAndComputeEvForBackAndLay() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        TextField txtBack1 = getField(controller, "txtLiveBack1");
        TextField txtLay1 = getField(controller, "txtLiveLay1");
        Label lblEvBack1 = getField(controller, "lblLiveEvBack1");
        Label lblEvLay1 = getField(controller, "lblLiveEvLay1");

        // Enter generous Back odds (e.g. 4.00 for Home win when probability is ~60%) -> EV Back > 0
        txtBack1.setText("4.00");
        assertThat(lblEvBack1.getText()).startsWith("+");
        assertThat(lblEvBack1.getStyleClass()).contains("badge", "badge-ev-positive");

        // Clear Back and enter very tight Lay odds (e.g. 1.05) -> EV Lay (Risk-Adjusted) > 0
        txtBack1.clear();
        txtLay1.setText("1.05");
        assertThat(lblEvLay1.getText()).startsWith("+");
        assertThat(lblEvLay1.getStyleClass()).contains("badge", "badge-ev-positive");

        // Enter unfavorable Back odds -> EV negative, badge removed
        txtBack1.setText("1.05");
        assertThat(lblEvBack1.getText()).startsWith("-");
        assertThat(lblEvBack1.getStyleClass()).doesNotContain("badge-ev-positive");
    }

    @Test
    @DisplayName("Green-Up evaluation for Long position (Punta -> Banca) should trigger on target met")
    void shouldEvaluateGreenUpForLongPositionCorrectly() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        TextField txtEntryOdds = getField(controller, "txtEntryOdds");
        ComboBox<String> comboEntryOutcome = getField(controller, "comboEntryOutcome");
        TextField txtLiveLay1 = getField(controller, "txtLiveLay1");
        Label lblProfit = getField(controller, "lblLiveGreenUpProfit");
        HBox boxBanner = getField(controller, "boxGreenUpBanner");
        Label lblBannerText = getField(controller, "lblGreenUpText");

        // Entry Long: Back at 2.40, current Lay at 1.50 -> profitRatio = (2.40 - 1.50) / 1.50 = +60.0%
        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_LONG);
        comboEntryOutcome.setValue("1");
        txtEntryOdds.setText("2.40");
        txtLiveLay1.setText("1.50");

        assertThat(lblProfit.getText()).contains("+60.0%");
        assertThat(boxBanner.isVisible()).isTrue();
        assertThat(lblBannerText.getText()).contains("Cash Out Punta → Banca").contains("+60.0%");
    }

    @Test
    @DisplayName("Green-Up evaluation for Short position (Banca -> Punta) should trigger on target met")
    void shouldEvaluateGreenUpForShortPositionCorrectly() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        TextField txtEntryOdds = getField(controller, "txtEntryOdds");
        ComboBox<String> comboEntryOutcome = getField(controller, "comboEntryOutcome");
        TextField txtLiveBack1 = getField(controller, "txtLiveBack1");
        Label lblProfit = getField(controller, "lblLiveGreenUpProfit");
        HBox boxBanner = getField(controller, "boxGreenUpBanner");
        Label lblBannerText = getField(controller, "lblGreenUpText");

        // Entry Short: Lay at 1.50, current Back at 3.00 -> profitRatio = 1.0 - (1.50 / 3.00) = +50.0%
        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_SHORT);
        comboEntryOutcome.setValue("1");
        txtEntryOdds.setText("1.50");
        txtLiveBack1.setText("3.00");

        assertThat(lblProfit.getText()).contains("+50.0%");
        assertThat(boxBanner.isVisible()).isTrue();
        assertThat(lblBannerText.getText()).contains("Cash Out Banca → Punta").contains("+50.0%");
    }

    @Test
    @DisplayName("Should display appropriate waiting label when required exit odds are missing")
    void shouldShowWaitingLabelWhenOppositeOddsNotEntered() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        TextField txtEntryOdds = getField(controller, "txtEntryOdds");
        Label lblProfit = getField(controller, "lblLiveGreenUpProfit");

        // Long without Lay odds
        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_LONG);
        txtEntryOdds.setText("2.50");
        assertThat(lblProfit.getText()).isEqualTo(LiveConsoleController.MSG_WAITING_BANCA_ODDS);

        // Short without Back odds
        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_SHORT);
        txtEntryOdds.setText("1.80");
        assertThat(lblProfit.getText()).isEqualTo(LiveConsoleController.MSG_WAITING_PUNTA_ODDS);
    }

    @Test
    @DisplayName("handleClearLiveOdds should clear volatile inputs from in-memory controls")
    void shouldHandleClearLiveOddsActionSuccessfully() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        TextField txtBack1 = getField(controller, "txtLiveBack1");
        TextField txtLay1 = getField(controller, "txtLiveLay1");
        TextField txtBackUnder = getField(controller, "txtLiveBackUnder");
        Label lblStatus = getField(controller, "lblStatus");

        txtBack1.setText("2.10");
        txtLay1.setText("2.14");
        txtBackUnder.setText("1.85");

        controller.handleClearLiveOdds(new ActionEvent());

        assertThat(txtBack1.getText()).isEmpty();
        assertThat(txtLay1.getText()).isEmpty();
        assertThat(txtBackUnder.getText()).isEmpty();
        assertThat(lblStatus.getText()).contains("Quote live in memoria azzerate");
    }

    @Test
    @DisplayName("Switching match should reset volatile quick odds and entry type to PUNTA (Long)")
    void shouldResetVolatileInputsOnMatchChange() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        TextField txtBack1 = getField(controller, "txtLiveBack1");

        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_SHORT);
        txtBack1.setText("2.50");

        // Load new or updated match details
        controller.loadMatchDetails(match);

        assertThat(comboEntryType.getValue()).isEqualTo(LiveConsoleController.ENTRY_TYPE_LONG);
        assertThat(txtBack1.getText()).isEmpty();
    }

    @Test
    @DisplayName("Long position Green-Up below target or negative should not display alert banner")
    void shouldNotShowBannerWhenTargetNotMetOrProfitNegative() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        TextField txtEntryOdds = getField(controller, "txtEntryOdds");
        TextField txtLiveLay1 = getField(controller, "txtLiveLay1");
        Label lblProfit = getField(controller, "lblLiveGreenUpProfit");
        HBox boxBanner = getField(controller, "boxGreenUpBanner");

        // Long entry at 2.00, current Lay at 1.90 -> profit +5.3%, below default target of 20%
        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_LONG);
        txtEntryOdds.setText("2.00");
        txtLiveLay1.setText("1.90");

        assertThat(lblProfit.getText()).contains("+5.3%");
        assertThat(boxBanner.isVisible()).isFalse();

        // Long entry at 2.00, current Lay at 2.50 -> negative profit -20.0%
        txtLiveLay1.setText("2.50");
        assertThat(lblProfit.getText()).contains("-20.0%");
        assertThat(boxBanner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Short position Green-Up below target or negative should not display alert banner")
    void shouldNotShowBannerForShortPositionWhenTargetNotMetOrNegative() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();
        controller.loadMatchDetails(match);

        ComboBox<String> comboEntryType = getField(controller, "comboEntryType");
        TextField txtEntryOdds = getField(controller, "txtEntryOdds");
        TextField txtLiveBack1 = getField(controller, "txtLiveBack1");
        Label lblProfit = getField(controller, "lblLiveGreenUpProfit");
        HBox boxBanner = getField(controller, "boxGreenUpBanner");

        // Short entry (Lay) at 2.00, current Back at 2.20 -> profit = 1 - (2.00 / 2.20) = +9.1%, below default 20%
        comboEntryType.setValue(LiveConsoleController.ENTRY_TYPE_SHORT);
        txtEntryOdds.setText("2.00");
        txtLiveBack1.setText("2.20");

        assertThat(lblProfit.getText()).contains("+9.1%");
        assertThat(boxBanner.isVisible()).isFalse();

        // Short entry at 2.00, current Back at 1.50 -> negative profit = 1 - (2.00 / 1.50) = -33.3%
        txtLiveBack1.setText("1.50");
        assertThat(lblProfit.getText()).contains("-33.3%");
        assertThat(boxBanner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Ticket 14: Live console should dynamically retrieve competition home advantage and respect neutral venue flag")
    void shouldRetrieveDynamicHomeAdvantageAndHandleNeutralVenue() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        when(manageMatchUseCase.getDynamicHomeAdvantage(10, 20)).thenReturn(1.35);

        controller.initialize();
        MatchDetailsDTO standardMatch = createSampleLiveMatch(); // isNeutralVenue is false, comp 10, season 20
        controller.loadMatchDetails(standardMatch);

        verify(manageMatchUseCase, atLeastOnce()).getDynamicHomeAdvantage(10, 20);

        // Now test neutral venue match
        MatchDetailsDTO neutralMatch = new MatchDetailsDTO(
                2,
                Instant.parse("2026-09-12T18:00:00Z"),
                MatchState.LIVE,
                false,
                0, 0,
                0, 0,
                0, 0,
                0, 0,
                null, null,
                null, null, null,
                true, false, false, false, false, // isNeutralVenue = true
                1.0, 1.0, 1.0, 1.0,
                0,
                10, "I1", "Serie A", "Italy", -0.12,
                20, "2024/2025",
                101, "Inter",
                102, "Milan"
        );

        controller.loadMatchDetails(neutralMatch);
        Label lblResidual = getField(controller, "lblResidualRates");
        assertThat(lblResidual.getText()).isNotEmpty();
    }

    @Test
    @DisplayName("Ticket 15: Pre-match rates should be cached on loadMatchDetails and not re-queried during live odds or minute updates")
    void shouldCachePreMatchRatesAndAvoidSynchronousDbQueriesOnLiveOddsChanges() {
        LiveConsoleController controller = createController();
        initAllControls(controller);
        setupStandardMocks();

        controller.initialize();
        MatchDetailsDTO match = createSampleLiveMatch();

        // 1. Load match details: pre-match rates must be computed and cached once
        controller.loadMatchDetails(match);

        assertThat(controller.getCachedLambdaPre()).isPositive();
        assertThat(controller.getCachedMuPre()).isPositive();
        assertThat(controller.getCachedModifiers()).isNotNull();

        // Verify initial invocation count during loadMatchDetails (1 home history, 1 away history, 1 league avg, 1 dynamic home adv, 1 stored odds)
        verify(manageMatchUseCase, times(1)).getHistoricalTeamPerformances(match.homeTeamId(), match.competitionId(), match.seasonId(), 10);
        verify(manageMatchUseCase, times(1)).getHistoricalTeamPerformances(match.awayTeamId(), match.competitionId(), match.seasonId(), 10);
        verify(manageMatchUseCase, times(1)).getLeagueAverageXgPerTeam(match.competitionId(), match.seasonId());
        verify(manageMatchUseCase, times(1)).getDynamicHomeAdvantage(match.competitionId(), match.seasonId());
        verify(manageMarketOddsUseCase, times(1)).getOddsForMatch(match.matchId());

        // 2. Perform frequent in-game actions: live odds typing, minute increment, odds clear
        TextField txtBack1 = getField(controller, "txtLiveBack1");
        TextField txtLay1 = getField(controller, "txtLiveLay1");
        TextField txtBackX = getField(controller, "txtLiveBackX");

        txtBack1.setText("2.50");
        txtLay1.setText("2.54");
        txtBackX.setText("3.40");
        controller.handlePlus1Min(new ActionEvent());
        controller.handlePlus5Min(new ActionEvent());
        controller.handleClearLiveOdds(new ActionEvent());

        // 3. Verify that despite 6 reactive recalculations, zero additional database queries were made
        verify(manageMatchUseCase, times(1)).getHistoricalTeamPerformances(match.homeTeamId(), match.competitionId(), match.seasonId(), 10);
        verify(manageMatchUseCase, times(1)).getHistoricalTeamPerformances(match.awayTeamId(), match.competitionId(), match.seasonId(), 10);
        verify(manageMatchUseCase, times(1)).getLeagueAverageXgPerTeam(match.competitionId(), match.seasonId());
        verify(manageMatchUseCase, times(1)).getDynamicHomeAdvantage(match.competitionId(), match.seasonId());
        verify(manageMarketOddsUseCase, times(1)).getOddsForMatch(match.matchId());

        // Residual rates UI should still be updated reactively
        Label lblResidual = getField(controller, "lblResidualRates");
        assertThat(lblResidual.getText()).isNotEmpty();
    }

    // --- Helper Methods ---

    private LiveConsoleController createController() {
        return new LiveConsoleController(
                liveMatchTradingUseCase,
                calculateLiveInferenceUseCase,
                manageMatchUseCase,
                manageMarketOddsUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );
    }

    private void setupStandardMocks() {
        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        new TeamStrengthCalculator.MatchPerformance(1.6, 1.0, false),
                        new TeamStrengthCalculator.MatchPerformance(1.4, 0.9, false)
                ));
        when(manageMarketOddsUseCase.getOddsForMatch(anyInt())).thenReturn(Collections.emptyList());
        when(liveMatchTradingUseCase.getMatchEvents(anyInt())).thenReturn(Collections.emptyList());
    }

    private MatchDetailsDTO createSampleLiveMatch() {
        return new MatchDetailsDTO(
                1,
                Instant.parse("2026-09-12T18:00:00Z"),
                MatchState.LIVE,
                false,
                1, 0,
                5, 2,
                3, 1,
                0, 0,
                null, null,
                2.20, 3.40, 3.50,
                false, false, false, false, false,
                1.0, 1.0, 1.0, 1.0,
                20,
                10, "I1", "Serie A", "Italy", -0.12,
                20, "2024/2025",
                101, "Inter",
                102, "Milan"
        );
    }

    private void initAllControls(LiveConsoleController controller) {
        setField(controller, "lblMatchHeader", new Label());
        setField(controller, "lblMatchInfo", new Label());
        setField(controller, "lblHomeTeamName", new Label());
        setField(controller, "lblAwayTeamName", new Label());
        setField(controller, "lblScore", new Label());
        setField(controller, "txtCurrentMinute", new TextField());
        setField(controller, "lblHomeRedCards", new Label());
        setField(controller, "lblAwayRedCards", new Label());
        setField(controller, "lblStatus", new Label());
        setField(controller, "lstEventsLog", new ListView<String>());
        setField(controller, "comboLiveMatchSelector", new ComboBox<MatchDetailsDTO>());

        setField(controller, "btnStartLive", new Button());
        setField(controller, "btnFinishMatch", new Button());
        setField(controller, "btnClearLiveOdds", new Button());

        setField(controller, "lblQuickFairOdds1", new Label());
        setField(controller, "txtLiveBack1", new TextField());
        setField(controller, "txtLiveLay1", new TextField());
        setField(controller, "lblLiveEvBack1", new Label());
        setField(controller, "lblLiveEvLay1", new Label());

        setField(controller, "lblQuickFairOddsX", new Label());
        setField(controller, "txtLiveBackX", new TextField());
        setField(controller, "txtLiveLayX", new TextField());
        setField(controller, "lblLiveEvBackX", new Label());
        setField(controller, "lblLiveEvLayX", new Label());

        setField(controller, "lblQuickFairOdds2", new Label());
        setField(controller, "txtLiveBack2", new TextField());
        setField(controller, "txtLiveLay2", new TextField());
        setField(controller, "lblLiveEvBack2", new Label());
        setField(controller, "lblLiveEvLay2", new Label());

        setField(controller, "comboLiveUoLine", new ComboBox<String>());
        setField(controller, "lblQuickTitleUnder", new Label());
        setField(controller, "lblQuickFairOddsUnder", new Label());
        setField(controller, "txtLiveBackUnder", new TextField());
        setField(controller, "txtLiveLayUnder", new TextField());
        setField(controller, "lblLiveEvBackUnder", new Label());
        setField(controller, "lblLiveEvLayUnder", new Label());

        setField(controller, "lblQuickTitleOver", new Label());
        setField(controller, "lblQuickFairOddsOver", new Label());
        setField(controller, "txtLiveBackOver", new TextField());
        setField(controller, "txtLiveLayOver", new TextField());
        setField(controller, "lblLiveEvBackOver", new Label());
        setField(controller, "lblLiveEvLayOver", new Label());

        setField(controller, "comboEntryType", new ComboBox<String>());
        setField(controller, "comboEntryOutcome", new ComboBox<String>());
        setField(controller, "txtEntryOdds", new TextField());
        setField(controller, "lblLiveGreenUpProfit", new Label());
        setField(controller, "boxGreenUpBanner", new HBox());
        setField(controller, "lblGreenUpText", new Label());

        setField(controller, "lblResidualRates", new Label());
        setField(controller, "lblLiveProb1", new Label());
        setField(controller, "lblLiveProbX", new Label());
        setField(controller, "lblLiveProb2", new Label());
        setField(controller, "lblLiveProbBtts", new Label());
        setField(controller, "lblLiveFairOdds1", new Label());
        setField(controller, "lblLiveFairOddsX", new Label());
        setField(controller, "lblLiveFairOdds2", new Label());
        setField(controller, "lblLiveFairOddsBtts", new Label());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not inject field: " + fieldName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Could not read field: " + fieldName, e);
        }
    }
}
