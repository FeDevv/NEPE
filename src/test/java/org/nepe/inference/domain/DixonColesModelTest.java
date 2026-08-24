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

@DisplayName("DixonColesModel Unit Tests")
class DixonColesModelTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Tau Adjustment Factor Tests")
    class TauFactorTests {

        @Test
        @DisplayName("Score (0,0) should have tau = 1 - lambda * mu * rho")
        void shouldCalculateTau00() {
            double lambda = 1.5;
            double mu = 1.2;
            double rho = -0.12;

            double expected = 1.0 - (lambda * mu * rho); // 1 - (1.8 * -0.12) = 1 + 0.216 = 1.216
            double actual = DixonColesModel.tau(0, 0, lambda, mu, rho);

            assertThat(actual).isCloseTo(expected, within(EPSILON));
            assertThat(actual).isGreaterThan(1.0); // Negative rho increases 0-0
        }

        @Test
        @DisplayName("Score (1,0) should have tau = 1 + mu * rho")
        void shouldCalculateTau10() {
            double lambda = 1.5;
            double mu = 1.2;
            double rho = -0.12;

            double expected = 1.0 + (mu * rho); // 1 + (1.2 * -0.12) = 1 - 0.144 = 0.856
            double actual = DixonColesModel.tau(1, 0, lambda, mu, rho);

            assertThat(actual).isCloseTo(expected, within(EPSILON));
            assertThat(actual).isLessThan(1.0); // Negative rho decreases 1-0
        }

        @Test
        @DisplayName("Score (0,1) should have tau = 1 + lambda * rho")
        void shouldCalculateTau01() {
            double lambda = 1.5;
            double mu = 1.2;
            double rho = -0.12;

            double expected = 1.0 + (lambda * rho); // 1 + (1.5 * -0.12) = 1 - 0.18 = 0.82
            double actual = DixonColesModel.tau(0, 1, lambda, mu, rho);

            assertThat(actual).isCloseTo(expected, within(EPSILON));
            assertThat(actual).isLessThan(1.0); // Negative rho decreases 0-1
        }

        @Test
        @DisplayName("Score (1,1) should have tau = 1 - rho")
        void shouldCalculateTau11() {
            double lambda = 1.5;
            double mu = 1.2;
            double rho = -0.12;

            double expected = 1.0 - rho; // 1 - (-0.12) = 1.12
            double actual = DixonColesModel.tau(1, 1, lambda, mu, rho);

            assertThat(actual).isCloseTo(expected, within(EPSILON));
            assertThat(actual).isGreaterThan(1.0); // Negative rho increases 1-1
        }

        @ParameterizedTest(name = "x: {0}, y: {1} -> tau should be 1.0")
        @CsvSource({
                "2, 0",
                "0, 2",
                "2, 1",
                "1, 2",
                "2, 2",
                "3, 0",
                "0, 3"
        })
        void shouldReturnOneForScoresGreaterThanOne(int x, int y) {
            double actual = DixonColesModel.tau(x, y, 1.5, 1.2, -0.12);
            assertThat(actual).isCloseTo(1.0, within(EPSILON));
        }

        @Test
        @DisplayName("Should throw when x or y is negative")
        void shouldThrowWhenScoreNegative() {
            assertThatThrownBy(() -> DixonColesModel.tau(-1, 0, 1.0, 1.0, -0.12))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> DixonColesModel.tau(0, -1, 1.0, 1.0, -0.12))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw when rho is out of bounds (-1.0 to 1.0)")
        void shouldThrowWhenRhoOutOfBounds() {
            assertThatThrownBy(() -> DixonColesModel.tau(0, 0, 1.0, 1.0, -1.0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> DixonColesModel.tau(0, 0, 1.0, 1.0, 1.0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> DixonColesModel.tau(0, 0, 1.0, 1.0, Double.NaN))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Dixon-Coles Score Matrix and Comparative Behavior")
    class ScoreMatrixTests {

        @Test
        @DisplayName("Dixon-Coles matrix should sum to 1.0 and shift low-score probabilities")
        void shouldAdjustProbabilitiesCorrectly() {
            double lambda = 1.4;
            double mu = 1.1;
            double rho = -0.12;

            double[][] dcMatrix = DixonColesModel.calculateScoreMatrix(lambda, mu, rho, 9);
            double[][] poisMatrix = PoissonModel.calculateScoreMatrix(lambda, mu, 9);

            // Verify normalization
            double dcSum = 0.0;
            for (double[] row : dcMatrix) {
                for (double cell : row) {
                    dcSum += cell;
                }
            }
            assertThat(dcSum).isCloseTo(1.0, within(EPSILON));

            // With negative rho:
            // P(0,0) and P(1,1) in DC must be greater than in pure Poisson
            assertThat(dcMatrix[0][0]).isGreaterThan(poisMatrix[0][0]);
            assertThat(dcMatrix[1][1]).isGreaterThan(poisMatrix[1][1]);

            // P(1,0) and P(0,1) in DC must be less than in pure Poisson
            assertThat(dcMatrix[1][0]).isLessThan(poisMatrix[1][0]);
            assertThat(dcMatrix[0][1]).isLessThan(poisMatrix[0][1]);
        }

        @Test
        @DisplayName("rho = 0 should produce matrix virtually identical to pure Poisson")
        void shouldMatchPoissonWhenRhoIsZero() {
            double lambda = 1.5;
            double mu = 1.2;

            double[][] dcMatrix = DixonColesModel.calculateScoreMatrix(lambda, mu, 0.0, 9);
            double[][] poisMatrix = PoissonModel.calculateScoreMatrix(lambda, mu, 9);

            for (int h = 0; h < dcMatrix.length; h++) {
                for (int a = 0; a < dcMatrix[h].length; a++) {
                    assertThat(dcMatrix[h][a]).isCloseTo(poisMatrix[h][a], within(1e-5));
                }
            }
        }

        @Test
        @DisplayName("Market delegation methods should compute valid coherent probabilities")
        void shouldComputeCoherentMarketProbabilities() {
            double[][] matrix = DixonColesModel.calculateScoreMatrix(1.5, 1.2, -0.12);

            double p1 = DixonColesModel.calculateHomeWinProbability(matrix);
            double pX = DixonColesModel.calculateDrawProbability(matrix);
            double p2 = DixonColesModel.calculateAwayWinProbability(matrix);
            assertThat(p1 + pX + p2).isCloseTo(1.0, within(EPSILON));

            double pUnder25 = DixonColesModel.calculateUnderProbability(matrix, 2.5);
            double pOver25 = DixonColesModel.calculateOverProbability(matrix, 2.5);
            assertThat(pUnder25 + pOver25).isCloseTo(1.0, within(EPSILON));

            double pBttsYes = DixonColesModel.calculateBttsYesProbability(matrix);
            double pBttsNo = DixonColesModel.calculateBttsNoProbability(matrix);
            assertThat(pBttsYes + pBttsNo).isCloseTo(1.0, within(EPSILON));

            assertThat(DixonColesModel.calculateFairOdds(p1)).isGreaterThan(1.0);
        }
    }
}
