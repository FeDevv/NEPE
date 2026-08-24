package org.nepe.inference.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("PoissonModel Unit Tests")
class PoissonModelTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Univariate Poisson Probability Tests")
    class UnivariateProbabilityTests {

        @Test
        @DisplayName("lambda = 0 should yield P(0) = 1.0 and P(k > 0) = 0.0")
        void shouldHandleZeroLambda() {
            assertThat(PoissonModel.probability(0.0, 0)).isCloseTo(1.0, within(EPSILON));
            assertThat(PoissonModel.probability(0.0, 1)).isCloseTo(0.0, within(EPSILON));
            assertThat(PoissonModel.probability(0.0, 5)).isCloseTo(0.0, within(EPSILON));
        }

        @ParameterizedTest(name = "lambda: {0}, k: {1} -> Expected P: {2}")
        @CsvSource({
                "1.0, 0, 0.36787944", // e^-1
                "1.0, 1, 0.36787944", // 1 * e^-1
                "1.0, 2, 0.18393972", // (1/2) * e^-1
                "2.0, 0, 0.13533528", // e^-2
                "2.0, 1, 0.27067056", // 2 * e^-2
                "2.0, 2, 0.27067056", // (4/2) * e^-2
                "2.0, 3, 0.18044704", // (8/6) * e^-2
                "1.5, 0, 0.22313016",
                "1.5, 1, 0.33469524",
                "1.5, 2, 0.25102143"
        })
        void shouldCalculateAccuratePoissonProbabilities(double lambda, int k, double expectedProb) {
            double actual = PoissonModel.probability(lambda, k);
            assertThat(actual).isCloseTo(expectedProb, within(1e-5));
        }

        @Test
        @DisplayName("Should throw exception when lambda is negative")
        void shouldThrowWhenLambdaNegative() {
            assertThatThrownBy(() -> PoissonModel.probability(-0.5, 1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception when lambda is NaN or Infinite")
        void shouldThrowWhenLambdaInvalid() {
            assertThatThrownBy(() -> PoissonModel.probability(Double.NaN, 0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> PoissonModel.probability(Double.POSITIVE_INFINITY, 0))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw exception when k is negative")
        void shouldThrowWhenKNegative() {
            assertThatThrownBy(() -> PoissonModel.probability(1.5, -1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot be negative");
        }
    }

    @Nested
    @DisplayName("Poisson 1D Distribution Tests")
    class DistributionTests {

        @Test
        @DisplayName("Distribution size should be maxGoals + 1 and sum close to 1.0 for large maxGoals")
        void shouldGenerateValidDistribution() {
            int maxGoals = 9;
            double lambda = 1.4;
            double[] dist = PoissonModel.distribution(lambda, maxGoals);

            assertThat(dist).hasSize(maxGoals + 1);

            double sum = 0.0;
            for (double p : dist) {
                assertThat(p).isGreaterThanOrEqualTo(0.0);
                sum += p;
            }
            assertThat(sum).isCloseTo(1.0, within(0.001)); // Truncated tail at k=9 for lambda=1.4 has mass ~ 1e-6
        }

        @Test
        @DisplayName("Distribution for lambda = 0 should have dist[0] = 1.0 and rest 0.0")
        void shouldHandleDistributionForZeroLambda() {
            double[] dist = PoissonModel.distribution(0.0, 5);
            assertThat(dist).hasSize(6);
            assertThat(dist[0]).isCloseTo(1.0, within(EPSILON));
            for (int i = 1; i <= 5; i++) {
                assertThat(dist[i]).isCloseTo(0.0, within(EPSILON));
            }
        }

        @Test
        @DisplayName("Should throw when maxGoals is out of bounds (< 1 or > 30)")
        void shouldThrowWhenMaxGoalsOutOfBounds() {
            assertThatThrownBy(() -> PoissonModel.distribution(1.5, 0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> PoissonModel.distribution(1.5, 31))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("2D Score Matrix and Market Aggregation Tests")
    class ScoreMatrixAndMarketsTests {

        @Test
        @DisplayName("Score matrix should be normalized and sum exactly to 1.0")
        void shouldGenerateNormalizedScoreMatrix() {
            double lambdaHome = 1.6;
            double muAway = 1.1;
            double[][] matrix = PoissonModel.calculateScoreMatrix(lambdaHome, muAway, 9);

            assertThat(matrix).hasDimensions(10, 10);

            double totalSum = 0.0;
            for (double[] row : matrix) {
                for (double cell : row) {
                    assertThat(cell).isGreaterThanOrEqualTo(0.0);
                    totalSum += cell;
                }
            }
            assertThat(totalSum).isCloseTo(1.0, within(EPSILON));
        }

        @Test
        @DisplayName("1X2 probabilities should be mutually exclusive and sum to 1.0")
        void shouldSum1X2ToUnity() {
            double[][] matrix = PoissonModel.calculateScoreMatrix(1.5, 1.2);

            double p1 = PoissonModel.calculateHomeWinProbability(matrix);
            double pX = PoissonModel.calculateDrawProbability(matrix);
            double p2 = PoissonModel.calculateAwayWinProbability(matrix);

            assertThat(p1).isGreaterThan(0.0);
            assertThat(pX).isGreaterThan(0.0);
            assertThat(p2).isGreaterThan(0.0);
            assertThat(p1 + pX + p2).isCloseTo(1.0, within(EPSILON));
        }

        @Test
        @DisplayName("Under + Over probabilities should sum to 1.0 for any threshold")
        void shouldSumUnderOverToUnity() {
            double[][] matrix = PoissonModel.calculateScoreMatrix(1.8, 1.3);

            double[] thresholds = {0.5, 1.5, 2.5, 3.5, 4.5};
            for (double threshold : thresholds) {
                double pUnder = PoissonModel.calculateUnderProbability(matrix, threshold);
                double pOver = PoissonModel.calculateOverProbability(matrix, threshold);

                assertThat(pUnder).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
                assertThat(pOver).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(1.0);
                assertThat(pUnder + pOver).isCloseTo(1.0, within(EPSILON));
            }
        }

        @Test
        @DisplayName("BTTS Yes + BTTS No should sum to 1.0")
        void shouldSumBttsToUnity() {
            double[][] matrix = PoissonModel.calculateScoreMatrix(1.5, 1.2);

            double pYes = PoissonModel.calculateBttsYesProbability(matrix);
            double pNo = PoissonModel.calculateBttsNoProbability(matrix);

            assertThat(pYes).isGreaterThan(0.0).isLessThan(1.0);
            assertThat(pNo).isGreaterThan(0.0).isLessThan(1.0);
            assertThat(pYes + pNo).isCloseTo(1.0, within(EPSILON));
        }

        @Test
        @DisplayName("Fair odds should be reciprocal of probability with boundary protection")
        void shouldCalculateCorrectFairOdds() {
            assertThat(PoissonModel.calculateFairOdds(0.50)).isCloseTo(2.00, within(EPSILON));
            assertThat(PoissonModel.calculateFairOdds(0.25)).isCloseTo(4.00, within(EPSILON));
            assertThat(PoissonModel.calculateFairOdds(0.00)).isCloseTo(1000.0, within(EPSILON));
            assertThat(PoissonModel.calculateFairOdds(1.00)).isCloseTo(1.00, within(EPSILON));
        }

        @Test
        @DisplayName("Should throw exception when calculating market on null or empty matrix")
        void shouldThrowOnInvalidMatrix() {
            assertThatThrownBy(() -> PoissonModel.calculateHomeWinProbability(null))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> PoissonModel.calculateDrawProbability(new double[0][0]))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw exception when Under/Over threshold is negative")
        void shouldThrowWhenThresholdNegative() {
            double[][] matrix = PoissonModel.calculateScoreMatrix(1.0, 1.0);
            assertThatThrownBy(() -> PoissonModel.calculateUnderProbability(matrix, -0.5))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
