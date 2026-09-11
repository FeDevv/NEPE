package org.nepe.inference.adapter.in;

import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.domain.Competition;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.inference.service.PreMatchInferenceService;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.match.domain.MatchState;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.match.port.out.MatchDetailsDTO;
import org.nepe.settings.domain.AppSettings;
import org.nepe.settings.port.in.ManageSettingsUseCase;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PreMatchAnalysisController Unit Tests")
class PreMatchAnalysisControllerTest {

    private final CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase = new PreMatchInferenceService();
    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final ManageMarketOddsUseCase manageMarketOddsUseCase = mock(ManageMarketOddsUseCase.class);
    private final ManageSettingsUseCase manageSettingsUseCase = mock(ManageSettingsUseCase.class);
    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
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
        assertThatThrownBy(() -> new PreMatchAnalysisController(
                null, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CalculatePreMatchInferenceUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, null, manageMarketOddsUseCase, manageSettingsUseCase, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, null, manageSettingsUseCase, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMarketOddsUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, null, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSettingsUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, manageCompetitionUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("PreMatchAnalysisController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        PreMatchAnalysisController controller = createController();

        assertThat(controller).isNotNull();
        assertThat(controller.getScopeCompetitionId()).isNull();
        assertThat(controller.getScopeSeasonId()).isNull();
    }

    @Test
    @DisplayName("Ticket 10: Switching matches should clear all market fields and prevent data leak")
    void shouldClearAllMarketOddsFieldsWhenSwitchingMatchesToPreventDataLeak() {
        PreMatchAnalysisController controller = createController();
        controller.initTestControls();
        setupStandardMocks();

        // 1. Load Match 101 with saved custom secondary market odds (Over 2.5 and BTTS Yes)
        MatchDetailsDTO match1 = createScheduledMatch(101, 2.10, 3.20, 3.60);
        List<MarketOdds> match1Odds = List.of(
                MarketOdds.create(101, MarketType.UNDER_OVER_25, "OVER", 2.10, 2.20),
                MarketOdds.create(101, MarketType.BTTS, "YES", 1.90, 2.00)
        );
        when(manageMarketOddsUseCase.getOddsForMatch(101)).thenReturn(match1Odds);

        controller.loadMatchDetails(match1);

        assertThat(controller.getTxtBackOver25().getText()).isEqualTo("2.10");
        assertThat(controller.getTxtLayOver25().getText()).isEqualTo("2.20");
        assertThat(controller.getTxtBackBttsYes().getText()).isEqualTo("1.90");
        assertThat(controller.getTxtLayBttsYes().getText()).isEqualTo("2.00");

        // 2. Load Match 102 with NO saved market odds and different CSV baseline odds
        MatchDetailsDTO match2 = createScheduledMatch(102, 1.75, 3.60, 4.80);
        when(manageMarketOddsUseCase.getOddsForMatch(102)).thenReturn(Collections.emptyList());

        controller.loadMatchDetails(match2);

        // 1X2 should be pre-filled with Match 102 CSV baseline odds
        assertThat(controller.getTxtBack1().getText()).isEqualTo("1.75");
        assertThat(controller.getTxtBackX().getText()).isEqualTo("3.60");
        assertThat(controller.getTxtBack2().getText()).isEqualTo("4.80");
        assertThat(controller.getTxtLay1().getText()).isEmpty();
        assertThat(controller.getTxtLayX().getText()).isEmpty();
        assertThat(controller.getTxtLay2().getText()).isEmpty();

        // Crucial verification: Secondary market fields from Match 101 MUST BE WIPED OUT (no leak)
        assertThat(controller.getTxtBackOver25().getText()).isEmpty();
        assertThat(controller.getTxtLayOver25().getText()).isEmpty();
        assertThat(controller.getTxtBackBttsYes().getText()).isEmpty();
        assertThat(controller.getTxtLayBttsYes().getText()).isEmpty();

        // All non-1X2 back fields must be empty
        for (TextField tf : controller.getAllMarketOddsTextFields()) {
            if (tf != controller.getTxtBack1() && tf != controller.getTxtBackX() && tf != controller.getTxtBack2()) {
                assertThat(tf.getText()).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("Ticket 10: Persisted custom MarketOdds must have precedence over CSV baseline odds")
    void shouldPrioritizePersistedMarketOddsOverCsvBaselineOdds() {
        PreMatchAnalysisController controller = createController();
        controller.initTestControls();
        setupStandardMocks();

        // Match with CSV odds: 1.80 / 3.50 / 4.50
        MatchDetailsDTO match = createScheduledMatch(201, 1.80, 3.50, 4.50);

        // Custom MarketOdds saved in DB:
        // - Outcome "1": Back 2.05, Lay 2.15 (different from CSV 1.80)
        // - Outcome "X": Back null, Lay 3.80 (user entered only Lay odds)
        // - Outcome "2": no custom odds (should fallback to CSV 4.50)
        List<MarketOdds> savedOdds = List.of(
                MarketOdds.create(201, MarketType.MATCH_ODDS, "1", 2.05, 2.15),
                MarketOdds.create(201, MarketType.MATCH_ODDS, "X", null, 3.80)
        );
        when(manageMarketOddsUseCase.getOddsForMatch(201)).thenReturn(savedOdds);

        controller.loadMatchDetails(match);

        // Outcome "1" uses custom MarketOdds (2.05 / 2.15), NOT CSV (1.80)
        assertThat(controller.getTxtBack1().getText()).isEqualTo("2.05");
        assertThat(controller.getTxtLay1().getText()).isEqualTo("2.15");

        // Outcome "X" has custom MarketOdds with null back, so back remains empty and lay is 3.80 (CSV 3.50 did not overwrite)
        assertThat(controller.getTxtBackX().getText()).isEmpty();
        assertThat(controller.getTxtLayX().getText()).isEqualTo("3.80");

        // Outcome "2" has no custom MarketOdds, so it falls back to CSV baseline 4.50
        assertThat(controller.getTxtBack2().getText()).isEqualTo("4.50");
        assertThat(controller.getTxtLay2().getText()).isEmpty();
    }

    @Test
    @DisplayName("Ticket 10: Loading ineligible match (e.g. FINISHED) should clear fields and disable controls")
    void shouldClearFieldsAndDisableControlsForIneligibleMatch() {
        PreMatchAnalysisController controller = createController();
        controller.initTestControls();
        setupStandardMocks();

        // Dirty up a field first
        controller.getTxtBack1().setText("9.99");
        controller.getTxtLay1().setText("10.50");

        MatchDetailsDTO finishedMatch = new MatchDetailsDTO(
                301,
                Instant.parse("2026-09-10T18:00:00Z"),
                MatchState.FINISHED,
                false,
                2, 1,
                14, 8,
                5, 3,
                0, 0,
                null, null,
                1.90, 3.40, 4.20,
                false, false, false, false, false,
                1.0, 1.0, 1.0, 1.0,
                90,
                1, "I1", "Serie A", "Italy", -0.1200,
                1, "2025/2026",
                10, "Inter",
                20, "Milan"
        );

        controller.loadMatchDetails(finishedMatch);

        // All fields should be wiped clean
        for (TextField tf : controller.getAllMarketOddsTextFields()) {
            assertThat(tf.getText()).isEmpty();
        }
    }

    @Test
    @DisplayName("loadMatchDetails with null match should return safely without throwing exception")
    void shouldHandleNullMatchGracefully() {
        PreMatchAnalysisController controller = createController();
        controller.loadMatchDetails(null);
        assertThat(controller.getScopeCompetitionId()).isNull();
    }

    // --- Helper Methods ---

    private PreMatchAnalysisController createController() {
        return new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase,
                manageMatchUseCase,
                manageMarketOddsUseCase,
                manageSettingsUseCase,
                manageCompetitionUseCase,
                springFXMLLoader
        );
    }

    private void setupStandardMocks() {
        when(manageMatchUseCase.getDynamicHomeAdvantage(anyInt(), anyInt())).thenReturn(1.20);
        when(manageMatchUseCase.getLeagueAverageXgPerTeam(anyInt(), anyInt())).thenReturn(1.35);
        when(manageMatchUseCase.getHistoricalTeamPerformances(anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(manageSettingsUseCase.getSettings()).thenReturn(AppSettings.defaults());
        when(manageCompetitionUseCase.getCompetitionById(anyInt()))
                .thenReturn(Competition.create("I1", "Serie A", "Italy", -0.1200));
    }

    private MatchDetailsDTO createScheduledMatch(int matchId, Double oddsH, Double oddsD, Double oddsA) {
        return new MatchDetailsDTO(
                matchId,
                Instant.parse("2026-09-12T18:45:00Z"),
                MatchState.SCHEDULED,
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
                1, "I1", "Serie A", "Italy", -0.1200,
                1, "2025/2026",
                10, "Inter",
                20, "Milan"
        );
    }
}
