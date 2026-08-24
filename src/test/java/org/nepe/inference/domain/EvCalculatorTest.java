package org.nepe.inference.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("EvCalculator Unit Tests")
class EvCalculatorTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("EV Back (Punta) Tests")
    class EvBackTests {

        @Test
        @DisplayName("Positive EV Back: P=0.55, K_Back=2.00, comm=0.05 -> EV = +0.0425 (+4.25%)")
        void shouldCalculatePositiveEvBack() {
            // winProfit = (2.0 - 1.0) * (1 - 0.05) = 0.95
            // EV = (0.55 * 0.95) - (0.45) = 0.5225 - 0.45 = +0.0725
            double ev = EvCalculator.calculateEvBack(0.55, 2.00, 0.05);
            assertThat(ev).isCloseTo(0.0725, within(EPSILON));
        }

        @Test
        @DisplayName("Negative EV Back: P=0.40, K_Back=2.00, comm=0.05 -> EV = -0.22 (-22.0%)")
        void shouldCalculateNegativeEvBack() {
            // winProfit = 1.0 * 0.95 = 0.95
            // EV = (0.40 * 0.95) - (0.60) = 0.38 - 0.60 = -0.22
            double ev = EvCalculator.calculateEvBack(0.40, 2.00, 0.05);
            assertThat(ev).isCloseTo(-0.22, within(EPSILON));
        }

        @Test
        @DisplayName("Zero Commission EV Back: P * K - 1")
        void shouldCalculateEvBackWithoutCommission() {
            double ev = EvCalculator.calculateEvBack(0.50, 2.20, 0.00);
            // EV = 0.50 * 1.20 - 0.50 = 0.60 - 0.50 = +0.10
            assertThat(ev).isCloseTo(0.10, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("EV Lay (Banca) and Risk-Adjusted Tests")
    class EvLayTests {

        @Test
        @DisplayName("Positive EV Lay: P=0.40, K_Lay=2.00, comm=0.05 -> EV = +0.17 (+17.0%)")
        void shouldCalculatePositiveEvLay() {
            // winStake = (1 - 0.40) * (1 - 0.05) = 0.60 * 0.95 = 0.57
            // lossLiability = 0.40 * (2.00 - 1.0) = 0.40
            // EV = 0.57 - 0.40 = +0.17
            double ev = EvCalculator.calculateEvLay(0.40, 2.00, 0.05);
            assertThat(ev).isCloseTo(0.17, within(EPSILON));
        }

        @Test
        @DisplayName("Risk-Adjusted EV Lay on liability: EV_Lay / (K_Lay - 1)")
        void shouldCalculateRiskAdjustedEvLay() {
            // For K_Lay = 3.00, liability = 2.00
            // winStake = (1 - 0.30) * 0.95 = 0.70 * 0.95 = 0.665
            // lossLiability = 0.30 * 2.00 = 0.60
            // EV_Lay = 0.665 - 0.60 = 0.065
            // EV_Risk = 0.065 / 2.00 = 0.0325 (+3.25% ROI on risked capital)
            double evRisk = EvCalculator.calculateEvLayRiskAdjusted(0.30, 3.00, 0.05);
            assertThat(evRisk).isCloseTo(0.0325, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Comprehensive Evaluation Record Tests")
    class EvaluateTests {

        @Test
        @DisplayName("Should evaluate both Back and Lay options and set value flags appropriately")
        void shouldEvaluateBothSides() {
            // P = 0.55 (fair odds ~ 1.82)
            // Market Back = 2.10 (Positive value)
            // Market Lay = 2.20 (Negative value for lay)
            EvCalculator.EvEvaluation eval = EvCalculator.evaluate(0.55, 2.10, 2.20, 0.05);

            assertThat(eval.evBack()).isNotNull();
            assertThat(eval.evLay()).isNotNull();
            assertThat(eval.evLayRiskAdjusted()).isNotNull();

            assertThat(eval.hasBackValue()).isTrue();
            assertThat(eval.hasLayValue()).isFalse();
            assertThat(eval.hasAnyValue()).isTrue();
        }

        @Test
        @DisplayName("Should handle single-sided odds evaluations (Back only or Lay only)")
        void shouldHandlePartialOdds() {
            EvCalculator.EvEvaluation backOnly = EvCalculator.evaluate(0.60, 2.00, null, 0.05);
            assertThat(backOnly.evBack()).isNotNull();
            assertThat(backOnly.evLay()).isNull();
            assertThat(backOnly.evLayRiskAdjusted()).isNull();
            assertThat(backOnly.hasBackValue()).isTrue();
            assertThat(backOnly.hasLayValue()).isFalse();

            EvCalculator.EvEvaluation layOnly = EvCalculator.evaluate(0.30, null, 2.50, 0.05);
            assertThat(layOnly.evBack()).isNull();
            assertThat(layOnly.evLay()).isNotNull();
            assertThat(layOnly.hasBackValue()).isFalse();
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class InvariantValidationTests {

        @Test
        @DisplayName("Should throw exception when probability is outside [0.0, 1.0]")
        void shouldThrowOnInvalidProbability() {
            assertThatThrownBy(() -> EvCalculator.calculateEvBack(-0.1, 2.0, 0.05))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> EvCalculator.calculateEvBack(1.1, 2.0, 0.05))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> EvCalculator.calculateEvBack(Double.NaN, 2.0, 0.05))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw exception when commission is outside [0.0, 1.0)")
        void shouldThrowOnInvalidCommission() {
            assertThatThrownBy(() -> EvCalculator.calculateEvBack(0.5, 2.0, -0.01))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> EvCalculator.calculateEvBack(0.5, 2.0, 1.00))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw exception when odds are <= 1.0")
        void shouldThrowOnInvalidOdds() {
            assertThatThrownBy(() -> EvCalculator.calculateEvBack(0.5, 1.00, 0.05))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> EvCalculator.calculateEvLay(0.5, 0.90, 0.05))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
