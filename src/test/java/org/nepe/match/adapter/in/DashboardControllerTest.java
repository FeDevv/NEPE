package org.nepe.match.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.inference.port.in.MarketPrediction;
import org.nepe.inference.port.in.PreMatchAnalysisResult;
import org.nepe.inference.port.in.PreMatchInferenceQuery;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.ImportCsvMatchesUseCase;
import org.nepe.match.port.in.LiveMatchTradingUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.settings.domain.AppSettings;
import org.nepe.shared.exception.EntityNotFoundException;

import org.mockito.ArgumentCaptor;
import org.nepe.inference.service.PreMatchInferenceService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final LiveMatchTradingUseCase liveMatchTradingUseCase = mock(LiveMatchTradingUseCase.class);
    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
    private final ManageSeasonUseCase manageSeasonUseCase = mock(ManageSeasonUseCase.class);
    private final ImportCsvMatchesUseCase importCsvMatchesUseCase = mock(ImportCsvMatchesUseCase.class);
    private final CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase = mock(CalculatePreMatchInferenceUseCase.class);
    private final org.nepe.settings.port.in.ManageSettingsUseCase manageSettingsUseCase = mock(org.nepe.settings.port.in.ManageSettingsUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @Test
    @DisplayName("Constructor should enforce non-null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new DashboardController(
                null, liveMatchTradingUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, liveMatchTradingUseCase, null, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, liveMatchTradingUseCase, manageCompetitionUseCase, null, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSeasonUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, liveMatchTradingUseCase, manageCompetitionUseCase, manageSeasonUseCase, null, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ImportCsvMatchesUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, liveMatchTradingUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, null, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CalculatePreMatchInferenceUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, liveMatchTradingUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSettingsUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, liveMatchTradingUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("DashboardController should construct properly with valid dependencies using 8-arg constructor")
    void shouldConstructWithValidDependencies8Arg() {
        DashboardController controller = new DashboardController(
                manageMatchUseCase,
                liveMatchTradingUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                calculatePreMatchInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
        assertThat(controller.getSelectedMatchIdForNavigation()).isNull();
        assertThat(controller.getCurrentCompetition()).isNull();
        assertThat(controller.getCurrentSeason()).isNull();
    }

    @Test
    @DisplayName("DashboardController should construct properly with valid dependencies using 7-arg constructor (backward compatibility)")
    void shouldConstructWithValidDependencies7Arg() {
        DashboardController controller = new DashboardController(
                manageMatchUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                calculatePreMatchInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
        assertThat(controller.getSelectedMatchIdForNavigation()).isNull();
        assertThat(controller.getCurrentCompetition()).isNull();
        assertThat(controller.getCurrentSeason()).isNull();
    }

    @Test
    @DisplayName("selectCompetitionAndSeason should handle uninitialized UI controls safely without throwing NPE")
    void shouldHandleSelectCompetitionAndSeasonSafelyWhenUninitialized() {
        DashboardController controller = new DashboardController(
                manageMatchUseCase,
                liveMatchTradingUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                calculatePreMatchInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        controller.selectCompetitionAndSeason(1, 2);
        controller.selectCompetitionAndSeason(null, null);

        assertThat(controller.getCurrentCompetition()).isNull();
        assertThat(controller.getCurrentSeason()).isNull();
    }

    @Test
    @DisplayName("handleStartLiveMatch should safely handle null match parameter without throwing NPE")
    void shouldHandleNullMatchInStartLiveMatchSafely() {
        DashboardController controller = new DashboardController(
                manageMatchUseCase,
                liveMatchTradingUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                calculatePreMatchInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        controller.handleStartLiveMatch(null);
        verifyNoInteractions(liveMatchTradingUseCase);
    }

    @Test
    @DisplayName("handleFinishMatch should safely handle null match parameter without throwing NPE")
    void shouldHandleNullMatchInFinishMatchSafely() {
        DashboardController controller = createController();

        controller.handleFinishMatch(null);
        verifyNoInteractions(manageMatchUseCase);
    }

    // --- Pre-Match Rigorous EV Signal Tests (Ticket 5) ---

    @Test
    @DisplayName("computePreMatchEvForTest should return '-' when match is null")
    void shouldReturnDashWhenMatchIsNull() {
        DashboardController controller = createController();
        String signal = controller.computePreMatchEvForTest(null);
        assertThat(signal).isEqualTo("-");
    }

    @Test
    @DisplayName("computePreMatchEvForTest should return '-' when match state does not allow pre-match analysis")
    void shouldReturnDashWhenMatchStateDoesNotAllowPreMatchAnalysis() {
        DashboardController controller = createController();

        MatchDetailsDTO finishedMatch = createTestMatchDto(1, MatchState.FINISHED, 2.00, 3.20, 3.80);
        MatchDetailsDTO cancelledMatch = createTestMatchDto(2, MatchState.CANCELLED, 2.00, 3.20, 3.80);
        MatchDetailsDTO liveMatch = createTestMatchDto(3, MatchState.LIVE, 2.00, 3.20, 3.80);

        assertThat(controller.computePreMatchEvForTest(finishedMatch)).isEqualTo("-");
        assertThat(controller.computePreMatchEvForTest(cancelledMatch)).isEqualTo("-");
        assertThat(controller.computePreMatchEvForTest(liveMatch)).isEqualTo("-");
    }

    @Test
    @DisplayName("computePreMatchEvForTest should return '-' when match has no reference odds")
    void shouldReturnDashWhenMatchHasNoOdds() {
        DashboardController controller = createController();
        MatchDetailsDTO matchWithoutOdds = createTestMatchDto(1, MatchState.SCHEDULED, null, null, null);

        assertThat(controller.computePreMatchEvForTest(matchWithoutOdds)).isEqualTo("-");
    }

    @Test
    @DisplayName("computePreMatchEvForTest should identify single EV+ value when Home Win exceeds threshold")
    void shouldComputePreMatchEvWhenHomeWinHasValue() {
        DashboardController controller = createController();
        MatchDetailsDTO match = createTestMatchDto(100, MatchState.SCHEDULED, 2.40, 3.20, 3.60);

        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);

        // Model estimated probabilities: P(Home)=0.50, P(Draw)=0.25, P(Away)=0.25
        // At oddsHome = 2.40, comm = 0.05:
        // EV_Back = 0.50 * (2.40 - 1.0) * 0.95 - (1.0 - 0.50) = 0.50 * 1.33 - 0.50 = 0.665 - 0.50 = +0.165 (+16.5%)
        MarketPrediction pHome = new MarketPrediction("1", 0.50, 2.00, null);
        MarketPrediction pDraw = new MarketPrediction("X", 0.25, 4.00, null);
        MarketPrediction pAway = new MarketPrediction("2", 0.25, 4.00, null);

        PreMatchAnalysisResult analysisResult = new PreMatchAnalysisResult(
                1.50, 1.00, 1.20, new double[10][10],
                pHome, pDraw, pAway, List.of(),
                new MarketPrediction("YES", 0.55, 1.82, null),
                new MarketPrediction("NO", 0.45, 2.22, null)
        );

        when(calculatePreMatchInferenceUseCase.calculate(any(PreMatchInferenceQuery.class)))
                .thenReturn(analysisResult);

        String signal = controller.computePreMatchEvForTest(match);
        assertThat(signal).isEqualTo("EV+ 1 (+16.5%)");

        ArgumentCaptor<PreMatchInferenceQuery> queryCaptor = ArgumentCaptor.forClass(PreMatchInferenceQuery.class);
        verify(calculatePreMatchInferenceUseCase).calculate(queryCaptor.capture());
        PreMatchInferenceQuery capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.seasonalDecayGamma()).isEqualTo(AppSettings.defaults().getSeasonalDecayGamma());
        assertThat(capturedQuery.dixonColesRho()).isEqualTo(match.dixonColesRho());
        assertThat(capturedQuery.commissionRate()).isEqualTo(AppSettings.defaults().getCommissionRate());
        assertThat(capturedQuery.homeAdvantageRatio()).isEqualTo(1.20);
        assertThat(capturedQuery.leagueAvgXgPerTeam()).isEqualTo(1.35);
    }

    @Test
    @DisplayName("computePreMatchEvForTest should select the highest EV when multiple outcomes exceed threshold")
    void shouldSelectHighestEvWhenMultipleOutcomesHaveValue() {
        DashboardController controller = createController();
        // Odds: 1=2.00 (EV=0.045), 2=5.00 (EV=+0.1875)
        MatchDetailsDTO match = createTestMatchDto(101, MatchState.SCHEDULED, 2.00, 3.00, 5.00);

        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);

        // P(Home)=0.55 -> EV_1 = 0.55 * 1.0 * 0.95 - 0.45 = 0.5225 - 0.45 = +0.0725 (+7.3%)
        // P(Draw)=0.20 -> EV_X = 0.20 * 2.0 * 0.95 - 0.80 = 0.38 - 0.80 = -0.42
        // P(Away)=0.25 -> EV_2 = 0.25 * 4.0 * 0.95 - 0.75 = 0.95 - 0.75 = +0.20 (+20.0%)
        MarketPrediction pHome = new MarketPrediction("1", 0.55, 1.82, null);
        MarketPrediction pDraw = new MarketPrediction("X", 0.20, 5.00, null);
        MarketPrediction pAway = new MarketPrediction("2", 0.25, 4.00, null);

        PreMatchAnalysisResult analysisResult = new PreMatchAnalysisResult(
                1.60, 1.10, 1.20, new double[10][10],
                pHome, pDraw, pAway, List.of(),
                new MarketPrediction("YES", 0.50, 2.00, null),
                new MarketPrediction("NO", 0.50, 2.00, null)
        );

        when(calculatePreMatchInferenceUseCase.calculate(any(PreMatchInferenceQuery.class)))
                .thenReturn(analysisResult);

        String signal = controller.computePreMatchEvForTest(match);
        assertThat(signal).isEqualTo("EV+ 2 (+20.0%)");
    }

    @Test
    @DisplayName("computePreMatchEvForTest should return '-' when all computed EV values are below threshold")
    void shouldReturnDashWhenAllEvBelowThreshold() {
        DashboardController controller = createController();
        // Fair odds match market odds exactly -> EV < 0 due to commission
        MatchDetailsDTO match = createTestMatchDto(102, MatchState.SCHEDULED, 2.00, 3.33, 5.00);

        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);

        MarketPrediction pHome = new MarketPrediction("1", 0.50, 2.00, null);
        MarketPrediction pDraw = new MarketPrediction("X", 0.30, 3.33, null);
        MarketPrediction pAway = new MarketPrediction("2", 0.20, 5.00, null);

        PreMatchAnalysisResult analysisResult = new PreMatchAnalysisResult(
                1.40, 0.90, 1.20, new double[10][10],
                pHome, pDraw, pAway, List.of(),
                new MarketPrediction("YES", 0.50, 2.00, null),
                new MarketPrediction("NO", 0.50, 2.00, null)
        );

        when(calculatePreMatchInferenceUseCase.calculate(any(PreMatchInferenceQuery.class)))
                .thenReturn(analysisResult);

        String signal = controller.computePreMatchEvForTest(match);
        assertThat(signal).isEqualTo("-");
    }

    @Test
    @DisplayName("computePreMatchEvForTest should gracefully handle DomainException without throwing")
    void shouldHandleDomainExceptionGracefully() {
        DashboardController controller = createController();
        MatchDetailsDTO match = createTestMatchDto(103, MatchState.SCHEDULED, 2.20, 3.20, 3.80);

        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("Team historical data not found"));

        String signal = controller.computePreMatchEvForTest(match);
        assertThat(signal).isEqualTo("-");
    }

    @Test
    @DisplayName("computePreMatchEvForTest should skip invalid odds <= 1.0 gracefully")
    void shouldHandleInvalidOddsGracefully() {
        DashboardController controller = createController();
        MatchDetailsDTO match = createTestMatchDto(104, MatchState.SCHEDULED, 1.00, 0.90, null);

        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);

        MarketPrediction pHome = new MarketPrediction("1", 0.50, 2.00, null);
        MarketPrediction pDraw = new MarketPrediction("X", 0.25, 4.00, null);
        MarketPrediction pAway = new MarketPrediction("2", 0.25, 4.00, null);

        PreMatchAnalysisResult analysisResult = new PreMatchAnalysisResult(
                1.50, 1.00, 1.20, new double[10][10],
                pHome, pDraw, pAway, List.of(),
                new MarketPrediction("YES", 0.50, 2.00, null),
                new MarketPrediction("NO", 0.50, 2.00, null)
        );

        when(calculatePreMatchInferenceUseCase.calculate(any(PreMatchInferenceQuery.class)))
                .thenReturn(analysisResult);

        String signal = controller.computePreMatchEvForTest(match);
        assertThat(signal).isEqualTo("-");
    }

    @Test
    @DisplayName("getPreMatchEvCache should return an unmodifiable view of the cache")
    void shouldExposeUnmodifiableCache() {
        DashboardController controller = createController();
        assertThat(controller.getPreMatchEvCache()).isEmpty();
        assertThatThrownBy(() -> controller.getPreMatchEvCache().put(1, "EV+ 1 (+5.0%)"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- Helper Factory Methods ---

    private DashboardController createController() {
        return new DashboardController(
                manageMatchUseCase,
                liveMatchTradingUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                calculatePreMatchInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );
    }

    private MatchDetailsDTO createTestMatchDto(int matchId, MatchState state, Double oddsH, Double oddsD, Double oddsA) {
        return new MatchDetailsDTO(
                matchId,
                Instant.parse("2026-09-10T18:00:00Z"),
                state,
                false,
                null, null,
                null, null,
                null, null,
                0, 0,
                null, null,
                oddsH, oddsD, oddsA,
                false, false, false, false, false,
                1.0, 1.0, 1.0, 1.0,
                0,
                1, "I1", "Serie A", "Italy", -0.12,
                1, "2025/2026",
                10, "Inter",
                20, "Milan"
        );
    }

    @Test
    @DisplayName("computePreMatchEvForTest should execute end-to-end with real PreMatchInferenceService without domain validation errors")
    void shouldWorkEndToEndWithRealInferenceServiceWithoutDomainExceptions() {
        CalculatePreMatchInferenceUseCase realInferenceUseCase = new PreMatchInferenceService();
        DashboardController realController = new DashboardController(
                manageMatchUseCase,
                liveMatchTradingUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                realInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        // High home odds (5.00) on favorite -> must yield a valid EV+ signal without throwing DomainValidationException
        MatchDetailsDTO match = createTestMatchDto(105, MatchState.SCHEDULED, 5.00, 3.40, 1.80);
        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);

        String signal = realController.computePreMatchEvForTest(match);
        assertThat(signal).isNotNull();
        assertThat(signal).startsWith("EV+ 1");
    }
}
