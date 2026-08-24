package org.nepe.inference.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.match.domain.MatchStatistics;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("LiveEngineModifiers Unit Tests")
class LiveEngineModifiersTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Time-Decay Tests")
    class TimeDecayTests {

        @ParameterizedTest(name = "Minute: {0} -> Factor: {1}")
        @CsvSource({
                "0,  1.00",
                "30, 0.66666667", // 60 / 90
                "45, 0.50",       // 45 / 90
                "60, 0.33333333", // 30 / 90
                "90, 0.00",
                "95, 0.00"        // Clamped at 90+
        })
        void shouldCalculateCorrectTimeDecay(int minute, double expectedFactor) {
            double factor = LiveEngineModifiers.calculateTimeDecayFactor(minute);
            assertThat(factor).isCloseTo(expectedFactor, within(1e-5));
        }

        @Test
        @DisplayName("Residual rates should drop to exactly 0.0 at and beyond minute 90")
        void shouldClampResidualRatesToZeroAtMinute90() {
            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.5, 1.2, 90, 0, 0, 0, 0, false, false, false, false
            );
            assertThat(rates.lambdaHomeResidual()).isCloseTo(0.0, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(0.0, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Red Card Tactical Adjustment Tests")
    class RedCardTests {

        @Test
        @DisplayName("1 Home Red Card: lambdaHome * 0.70, muAway * 1.30")
        void shouldApplySingleHomeRedCard() {
            // Minute 0 (decay = 1.0)
            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 0, 1, 0, 0, 0, false, false, false, false
            );

            assertThat(rates.lambdaHomeResidual()).isCloseTo(0.70, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(1.30, within(EPSILON));
        }

        @Test
        @DisplayName("2 Home Red Cards: cumulative power 0.70^2 and 1.30^2")
        void shouldApplyDoubleHomeRedCard() {
            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 0, 2, 0, 0, 0, false, false, false, false
            );

            assertThat(rates.lambdaHomeResidual()).isCloseTo(0.49, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(1.69, within(EPSILON));
        }

        @Test
        @DisplayName("1 Red Card each: mutually offsets (0.70 * 1.30 = 0.91)")
        void shouldApplyMutualRedCards() {
            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 0, 1, 1, 0, 0, false, false, false, false
            );

            assertThat(rates.lambdaHomeResidual()).isCloseTo(0.91, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(0.91, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Second-Half Must-Win Motivation Tests")
    class MustWinTests {

        @Test
        @DisplayName("Must-Win active in 2nd half when drawing (0-0 at min 50): +25% attack, +20% defense penalty")
        void shouldApplyMustWinWhenDrawingInSecondHalf() {
            // Time decay at min 50: 40/90 = 4/9
            double baseLambda = 1.8 * (40.0 / 90.0); // 0.80
            double baseMu = 0.9 * (40.0 / 90.0);     // 0.40

            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.8, 0.9, 50, 0, 0, 0, 0, true, false, false, false
            );

            // Home attack boosted: 0.80 * 1.25 = 1.00
            // Home defense compromised (Away attack boosted): 0.40 * 1.20 = 0.48
            assertThat(rates.lambdaHomeResidual()).isCloseTo(baseLambda * 1.25, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(baseMu * 1.20, within(EPSILON));
        }

        @Test
        @DisplayName("Must-Win should NOT trigger when team is already leading")
        void shouldNotApplyMustWinWhenLeading() {
            // Home leading 1-0 at min 60
            LiveEngineModifiers.LiveRates ratesWithMustWin = LiveEngineModifiers.calculateResidualRates(
                    1.5, 1.2, 60, 0, 0, 1, 0, true, false, false, false
            );
            LiveEngineModifiers.LiveRates ratesStandard = LiveEngineModifiers.calculateResidualRates(
                    1.5, 1.2, 60, 0, 0, 1, 0, false, false, false, false
            );

            assertThat(ratesWithMustWin.lambdaHomeResidual()).isCloseTo(ratesStandard.lambdaHomeResidual(), within(EPSILON));
            assertThat(ratesWithMustWin.muAwayResidual()).isCloseTo(ratesStandard.muAwayResidual(), within(EPSILON));
        }

        @Test
        @DisplayName("Must-Win should NOT trigger in the 1st half (min < 45)")
        void shouldNotApplyMustWinInFirstHalf() {
            LiveEngineModifiers.LiveRates ratesWithMustWin = LiveEngineModifiers.calculateResidualRates(
                    1.5, 1.2, 30, 0, 0, 0, 0, true, false, false, false
            );
            LiveEngineModifiers.LiveRates ratesStandard = LiveEngineModifiers.calculateResidualRates(
                    1.5, 1.2, 30, 0, 0, 0, 0, false, false, false, false
            );

            assertThat(ratesWithMustWin.lambdaHomeResidual()).isCloseTo(ratesStandard.lambdaHomeResidual(), within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Mutual Low-Urgency Tests")
    class LowUrgencyTests {

        @Test
        @DisplayName("Mutual Low-Urgency should reduce both rates by 35% (x 0.65)")
        void shouldApplyLowUrgencyDiscount() {
            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 0, 0, 0, 0, 0, false, false, true, true
            );

            assertThat(rates.lambdaHomeResidual()).isCloseTo(0.65, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(0.65, within(EPSILON));
        }

        @Test
        @DisplayName("One-sided Low-Urgency should NOT apply mutual discount")
        void shouldNotApplyDiscountWhenOneSided() {
            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 0, 0, 0, 0, 0, false, false, true, false
            );

            assertThat(rates.lambdaHomeResidual()).isCloseTo(1.00, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(1.00, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Value Object Overload Tests")
    class ValueObjectIntegrationTests {

        @Test
        @DisplayName("Should integrate properly with MatchStatistics and MatchModifiers")
        void shouldCalculateFromDomainObjects() {
            MatchStatistics stats = new MatchStatistics(1, 1, 8, 5, 3, 2, 1, 0, null, null);
            MatchModifiers mods = new MatchModifiers(false, true, false, false, false, 1.0, 1.0, 1.0, 1.0);

            LiveEngineModifiers.LiveRates rates = LiveEngineModifiers.calculateResidualRates(
                    1.8, 1.2, 60, stats, mods
            );

            // Min 60 -> decay 30/90 = 1/3
            // Home Red Card 1 -> Home * 0.70, Away * 1.30
            // Home Must-Win at 1-1 in 2nd half -> Home * 1.25, Away * 1.20
            double expectedHome = (1.8 / 3.0) * 0.70 * 1.25; // 0.60 * 0.70 * 1.25 = 0.525
            double expectedAway = (1.2 / 3.0) * 1.30 * 1.20; // 0.40 * 1.30 * 1.20 = 0.624

            assertThat(rates.lambdaHomeResidual()).isCloseTo(expectedHome, within(EPSILON));
            assertThat(rates.muAwayResidual()).isCloseTo(expectedAway, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class InvariantTests {

        @Test
        @DisplayName("Should throw when minute is outside [0, 130]")
        void shouldThrowOnInvalidMinute() {
            assertThatThrownBy(() -> LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, -1, 0, 0, 0, 0, false, false, false, false))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 131, 0, 0, 0, 0, false, false, false, false))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw when red cards are negative")
        void shouldThrowOnNegativeCards() {
            assertThatThrownBy(() -> LiveEngineModifiers.calculateResidualRates(
                    1.0, 1.0, 0, -1, 0, 0, 0, false, false, false, false))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
