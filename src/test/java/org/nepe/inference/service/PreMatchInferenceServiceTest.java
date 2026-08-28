package org.nepe.inference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.inference.domain.TeamStrengthCalculator.MatchPerformance;
import org.nepe.inference.port.in.MarketPrediction;
import org.nepe.inference.port.in.PreMatchAnalysisResult;
import org.nepe.inference.port.in.PreMatchInferenceQuery;
import org.nepe.match.domain.MarketOdds;
import org.nepe.match.domain.MarketType;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.shared.exception.DomainValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("PreMatchInferenceService Unit Tests")
class PreMatchInferenceServiceTest {

    private static final double EPSILON = 1e-5;
    private PreMatchInferenceService service;

    @BeforeEach
    void setUp() {
        service = new PreMatchInferenceService();
    }

    @Nested
    @DisplayName("Query-Based Calculation Tests")
    class QueryCalculationTests {

        @Test
        @DisplayName("Should execute complete pre-match pipeline and calculate accurate market probabilities")
        void shouldCalculateFromQuery() {
            List<MatchPerformance> homeMatches = List.of(
                    new MatchPerformance(2.1, 0.8, false),
                    new MatchPerformance(1.8, 1.1, false),
                    new MatchPerformance(2.4, 0.5, true) // previous season
            );

            List<MatchPerformance> awayMatches = List.of(
                    new MatchPerformance(1.0, 1.9, false),
                    new MatchPerformance(0.8, 2.2, false),
                    new MatchPerformance(1.2, 1.7, true)
            );

            List<MarketOdds> marketOdds = List.of(
                    MarketOdds.create(1, MarketType.MATCH_ODDS, "1", 2.20, 2.24),
                    MarketOdds.create(1, MarketType.MATCH_ODDS, "X", 3.40, 3.50),
                    MarketOdds.create(1, MarketType.MATCH_ODDS, "2", 3.60, 3.75),
                    MarketOdds.create(1, MarketType.UNDER_OVER_25, "OVER", 1.85, 1.90),
                    MarketOdds.create(1, MarketType.BTTS, "YES", 1.75, 1.80)
            );

            PreMatchInferenceQuery query = new PreMatchInferenceQuery(
                    homeMatches,
                    awayMatches,
                    1.35,
                    1.20,
                    0.70,
                    -0.12,
                    0.05,
                    MatchModifiers.defaultModifiers(),
                    marketOdds
            );

            PreMatchAnalysisResult result = service.calculate(query);

            assertThat(result.lambdaHome()).isPositive();
            assertThat(result.muAway()).isPositive();
            assertThat(result.effectiveHomeAdv()).isEqualTo(1.20);
            assertThat(result.scoreMatrix()).hasDimensions(10, 10);

            // Matrix sum must equal 1.0
            double sum = 0.0;
            for (double[] row : result.scoreMatrix()) {
                for (double val : row) {
                    sum += val;
                }
            }
            assertThat(sum).isCloseTo(1.0, within(EPSILON));

            // Match Odds (1X2) sum to 1.0
            double sum1X2 = result.homeWin().probability() + result.draw().probability() + result.awayWin().probability();
            assertThat(sum1X2).isCloseTo(1.0, within(EPSILON));

            // Under/Over markets (10 predictions: 5 thresholds x 2 outcomes)
            assertThat(result.underOverPredictions()).hasSize(10);

            // BTTS sum to 1.0
            double sumBtts = result.bttsYes().probability() + result.bttsNo().probability();
            assertThat(sumBtts).isCloseTo(1.0, within(EPSILON));

            // Verify EV calculation for supplied odds
            MarketPrediction homeWin = result.homeWin();
            assertThat(homeWin.evEvaluation()).isNotNull();
            assertThat(homeWin.evEvaluation().evBack()).isNotNull();
            assertThat(homeWin.evEvaluation().evLay()).isNotNull();
            assertThat(homeWin.evEvaluation().evLayRiskAdjusted()).isNotNull();
        }

        @Test
        @DisplayName("Should detect positive EV value opportunities correctly")
        void shouldDetectValueOpportunity() {
            // High back odds creating positive EV
            List<MarketOdds> marketOdds = List.of(
                    MarketOdds.create(1, MarketType.MATCH_ODDS, "1", 5.00, null)
            );

            PreMatchInferenceQuery query = new PreMatchInferenceQuery(
                    List.of(new MatchPerformance(2.5, 0.5, false)),
                    List.of(new MatchPerformance(0.5, 2.5, false)),
                    1.35,
                    1.20,
                    0.70,
                    -0.12,
                    0.05,
                    MatchModifiers.defaultModifiers(),
                    marketOdds
            );

            PreMatchAnalysisResult result = service.calculate(query);

            assertThat(result.homeWin().hasBackValue()).isTrue();
            assertThat(result.hasAnyValueOpportunity()).isTrue();
        }

        @Test
        @DisplayName("Should throw DomainValidationException when query is null")
        void shouldThrowWhenQueryIsNull() {
            assertThatThrownBy(() -> service.calculate(null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("PreMatchInferenceQuery cannot be null");
        }
    }

    @Nested
    @DisplayName("Direct Rate Calculation Tests")
    class DirectRateCalculationTests {

        @Test
        @DisplayName("Should calculate analysis directly from lambda and mu rates")
        void shouldCalculateDirectRates() {
            List<MarketOdds> odds = List.of(
                    MarketOdds.create(1, MarketType.BTTS, "YES", 1.90, 1.95)
            );

            PreMatchAnalysisResult result = service.calculate(1.65, 1.15, -0.12, 0.05, odds);

            assertThat(result.lambdaHome()).isEqualTo(1.65);
            assertThat(result.muAway()).isEqualTo(1.15);
            assertThat(result.effectiveHomeAdv()).isEqualTo(1.0);
            assertThat(result.bttsYes().evEvaluation()).isNotNull();
            assertThat(result.bttsNo().evEvaluation()).isNull(); // Odds not supplied for BTTS No
        }
    }
}
