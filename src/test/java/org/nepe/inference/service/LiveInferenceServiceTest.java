package org.nepe.inference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.inference.port.in.LiveAnalysisResult;
import org.nepe.inference.port.in.LiveInferenceQuery;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.shared.exception.DomainValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("LiveInferenceService Unit Tests")
class LiveInferenceServiceTest {

    private static final double EPSILON = 1e-5;
    private LiveInferenceService service;

    @BeforeEach
    void setUp() {
        service = new LiveInferenceService();
    }

    @Nested
    @DisplayName("In-Game Scenario Tests")
    class InGameScenarioTests {

        @Test
        @DisplayName("Minute 0: Should reproduce full-time pre-match probability baseline")
        void shouldHandleMinuteZero() {
            LiveInferenceQuery query = new LiveInferenceQuery(
                    1.60,
                    1.20,
                    0,
                    0,
                    0,
                    0,
                    0,
                    MatchModifiers.defaultModifiers(),
                    -0.12,
                    0.05,
                    0.10,
                    List.of(),
                    null
            );

            LiveAnalysisResult result = service.calculate(query);

            assertThat(result.lambdaHomeResidual()).isCloseTo(1.60, within(EPSILON));
            assertThat(result.muAwayResidual()).isCloseTo(1.20, within(EPSILON));

            double sum1X2 = result.finalHomeWin().probability() + result.finalDraw().probability() + result.finalAwayWin().probability();
            assertThat(sum1X2).isCloseTo(1.0, within(EPSILON));
        }

        @Test
        @DisplayName("Score 2-0 at minute 75: Home win should be high, Under 1.5 must be 0.0")
        void shouldAdjustProbabilitiesForCurrentScoreline() {
            LiveInferenceQuery query = new LiveInferenceQuery(
                    1.60,
                    1.20,
                    75, // 15 mins remaining (decay factor = 15/90 = 1/6)
                    2,
                    0,
                    0,
                    0,
                    MatchModifiers.defaultModifiers(),
                    -0.12,
                    0.05,
                    0.10,
                    List.of(),
                    null
            );

            LiveAnalysisResult result = service.calculate(query);

            // Residual rates should be heavily decayed
            assertThat(result.lambdaHomeResidual()).isCloseTo(1.60 * (15.0 / 90.0), within(EPSILON));
            assertThat(result.muAwayResidual()).isCloseTo(1.20 * (15.0 / 90.0), within(EPSILON));

            // Home is winning 2-0 with 15 mins left -> very high Home Win probability
            assertThat(result.finalHomeWin().probability()).isGreaterThan(0.85);

            // Since 2 goals are already scored, Under 0.5 and Under 1.5 are impossible (0.0)
            assertThat(result.underOverPredictions().get(0).outcome()).isEqualTo("UNDER"); // Under 0.5
            assertThat(result.underOverPredictions().get(0).probability()).isEqualTo(0.0);

            assertThat(result.underOverPredictions().get(2).outcome()).isEqualTo("UNDER"); // Under 1.5
            assertThat(result.underOverPredictions().get(2).probability()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Minute 92 (Injury time): Residual rates clamp to zero and result is locked")
        void shouldClampResidualRatesToZeroPastMinute90() {
            LiveInferenceQuery query = new LiveInferenceQuery(
                    2.00,
                    1.50,
                    93,
                    1,
                    1,
                    0,
                    0,
                    MatchModifiers.defaultModifiers(),
                    -0.12,
                    0.05,
                    0.10,
                    List.of(),
                    null
            );

            LiveAnalysisResult result = service.calculate(query);

            assertThat(result.lambdaHomeResidual()).isEqualTo(0.0);
            assertThat(result.muAwayResidual()).isEqualTo(0.0);

            // Score is 1-1 and no more goals are expected -> Draw is 100%
            assertThat(result.finalDraw().probability()).isEqualTo(1.0);
            assertThat(result.finalHomeWin().probability()).isEqualTo(0.0);
            assertThat(result.finalAwayWin().probability()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Red card & second-half Must-Win should adjust residual rates")
        void shouldApplyRedCardAndMustWinModifiers() {
            MatchModifiers modifiers = new MatchModifiers(
                    false,
                    true, // Must win Home
                    false,
                    false,
                    false,
                    1.0, 1.0, 1.0, 1.0
            );

            LiveInferenceQuery query = new LiveInferenceQuery(
                    1.80,
                    1.00,
                    60, // 2nd half -> must-win active since score is 0-1
                    0,
                    1,
                    1, // 1 red card for Home
                    0,
                    modifiers,
                    -0.12,
                    0.05,
                    0.10,
                    List.of(),
                    null
            );

            LiveAnalysisResult result = service.calculate(query);

            // Decay factor = 30/90 = 1/3
            // Home: 1.80 * (1/3) * 0.70 (red card) * 1.25 (must win attack) = 0.525
            assertThat(result.lambdaHomeResidual()).isCloseTo(0.525, within(EPSILON));

            // Away: 1.00 * (1/3) * 1.30 (red card bonus) * 1.20 (must win opponent defense penalty) = 0.520
            assertThat(result.muAwayResidual()).isCloseTo(0.520, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Green-Up and EV Tests")
    class GreenUpAndEvTests {

        @Test
        @DisplayName("Should trigger greenUpTargetMet when lay odds drop below threshold")
        void shouldTriggerGreenUpAlert() {
            // Entry Back odds: 3.00. Current live Lay odds: 2.00.
            // Profit ratio: (3.00 - 2.00) / 2.00 = 50% >= target (10%)
            List<MarketOdds> liveOdds = List.of(
                    MarketOdds.create(1, MarketType.MATCH_ODDS, "1", 1.95, 2.00)
            );

            LiveInferenceQuery query = new LiveInferenceQuery(
                    1.50,
                    1.00,
                    60,
                    1,
                    0,
                    0,
                    0,
                    MatchModifiers.defaultModifiers(),
                    -0.12,
                    0.05,
                    0.10, // 10% target
                    liveOdds,
                    3.00  // Entry odds
            );

            LiveAnalysisResult result = service.calculate(query);

            assertThat(result.greenUpTargetMet()).isTrue();
        }

        @Test
        @DisplayName("Should throw DomainValidationException when query is null")
        void shouldThrowWhenQueryIsNull() {
            assertThatThrownBy(() -> service.calculate(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("LiveInferenceQuery cannot be null");
        }
    }
}
