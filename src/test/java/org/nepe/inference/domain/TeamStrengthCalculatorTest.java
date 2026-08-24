package org.nepe.inference.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.match.domain.MatchModifiers;
import org.nepe.shared.exception.DomainValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TeamStrengthCalculator Unit Tests")
class TeamStrengthCalculatorTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Team Strength Calculation (Alpha / Beta) Tests")
    class StrengthCalculationTests {

        @Test
        @DisplayName("Empty match list should produce default neutral strength (alpha = 1.0, beta = 1.0)")
        void shouldHandleEmptyMatchList() {
            TeamStrengthCalculator.TeamStrength strength = TeamStrengthCalculator.calculateStrength(
                    List.of(), 1.35, 0.70
            );

            assertThat(strength.alphaOffense()).isCloseTo(1.00, within(EPSILON));
            assertThat(strength.betaDefense()).isCloseTo(1.00, within(EPSILON));
            assertThat(strength.weightedAvgXgScored()).isCloseTo(1.35, within(EPSILON));
            assertThat(strength.weightedAvgXgConceded()).isCloseTo(1.35, within(EPSILON));
        }

        @Test
        @DisplayName("Single match should have recency weight = 1.0")
        void shouldCalculateStrengthForSingleMatch() {
            List<TeamStrengthCalculator.MatchPerformance> matches = List.of(
                    new TeamStrengthCalculator.MatchPerformance(2.70, 0.675, false)
            );

            // leagueAvg = 1.35
            // alpha = 2.70 / 1.35 = 2.00
            // beta = 0.675 / 1.35 = 0.50
            TeamStrengthCalculator.TeamStrength strength = TeamStrengthCalculator.calculateStrength(
                    matches, 1.35, 0.70
            );

            assertThat(strength.alphaOffense()).isCloseTo(2.00, within(EPSILON));
            assertThat(strength.betaDefense()).isCloseTo(0.50, within(EPSILON));
        }

        @Test
        @DisplayName("Multiple matches: newest match should receive highest linear rank weight")
        void shouldApplyRecencyWeighting() {
            // Count = 2 matches.
            // Match 0 (newest): weight = 2/2 = 1.0
            // Match 1 (oldest): weight = 1/2 = 0.5
            // Total weights = 1.5
            List<TeamStrengthCalculator.MatchPerformance> matches = List.of(
                    new TeamStrengthCalculator.MatchPerformance(2.0, 1.0, false),
                    new TeamStrengthCalculator.MatchPerformance(1.0, 2.0, false)
            );

            // weightedScored = (2.0 * 1.0) + (1.0 * 0.5) = 2.5 -> avg = 2.5 / 1.5 = 1.666667
            // weightedConceded = (1.0 * 1.0) + (2.0 * 0.5) = 2.0 -> avg = 2.0 / 1.5 = 1.333333
            TeamStrengthCalculator.TeamStrength strength = TeamStrengthCalculator.calculateStrength(
                    matches, 1.00, 0.70
            );

            assertThat(strength.weightedAvgXgScored()).isCloseTo(1.666667, within(1e-5));
            assertThat(strength.weightedAvgXgConceded()).isCloseTo(1.333333, within(1e-5));
            assertThat(strength.alphaOffense()).isCloseTo(1.666667, within(1e-5));
            assertThat(strength.betaDefense()).isCloseTo(1.333333, within(1e-5));
        }

        @Test
        @DisplayName("Previous season matches should be discounted by seasonal decay gamma")
        void shouldApplySeasonalDecayGamma() {
            // Count = 2 matches.
            // Match 0 (current season, newest): weight = 1.0
            // Match 1 (previous season, oldest): weight = 0.5 * 0.70 (gamma) = 0.35
            // Total weights = 1.35
            List<TeamStrengthCalculator.MatchPerformance> matches = List.of(
                    new TeamStrengthCalculator.MatchPerformance(2.0, 1.0, false),
                    new TeamStrengthCalculator.MatchPerformance(1.0, 2.0, true)
            );

            // weightedScored = (2.0 * 1.0) + (1.0 * 0.35) = 2.35 -> avg = 2.35 / 1.35 = 1.74074
            TeamStrengthCalculator.TeamStrength strength = TeamStrengthCalculator.calculateStrength(
                    matches, 1.00, 0.70
            );

            assertThat(strength.weightedAvgXgScored()).isCloseTo(2.35 / 1.35, within(1e-5));
        }
    }

    @Nested
    @DisplayName("Pre-Match Rates (Lambda & Mu) Calculation Tests")
    class PreMatchRatesTests {

        @Test
        @DisplayName("Base calculation with Home Advantage")
        void shouldCalculateBasePreMatchRates() {
            TeamStrengthCalculator.TeamStrength home = new TeamStrengthCalculator.TeamStrength(1.20, 0.90, 1.62, 1.215);
            TeamStrengthCalculator.TeamStrength away = new TeamStrengthCalculator.TeamStrength(1.10, 1.00, 1.485, 1.35);

            double leagueAvg = 1.35;
            double homeAdv = 1.20; // Away disadv = 1 / 1.20 = 0.833333

            // lambda = alphaHome * betaAway * leagueAvg * homeAdv = 1.20 * 1.00 * 1.35 * 1.20 = 1.944
            // mu = alphaAway * betaHome * leagueAvg * (1/homeAdv) = 1.10 * 0.90 * 1.35 * (1/1.20) = 1.11375
            TeamStrengthCalculator.PreMatchRates rates = TeamStrengthCalculator.calculatePreMatchRates(
                    home, away, leagueAvg, homeAdv, MatchModifiers.defaultModifiers()
            );

            assertThat(rates.lambdaHome()).isCloseTo(1.944, within(EPSILON));
            assertThat(rates.muAway()).isCloseTo(1.11375, within(EPSILON));
            assertThat(rates.effectiveHomeAdvantage()).isCloseTo(1.20, within(EPSILON));
        }

        @Test
        @DisplayName("Neutral venue should disable Home Advantage (effective home advantage = 1.0)")
        void shouldHandleNeutralVenue() {
            TeamStrengthCalculator.TeamStrength home = new TeamStrengthCalculator.TeamStrength(1.0, 1.0, 1.35, 1.35);
            TeamStrengthCalculator.TeamStrength away = new TeamStrengthCalculator.TeamStrength(1.0, 1.0, 1.35, 1.35);

            MatchModifiers neutralMods = new MatchModifiers(true, false, false, false, false, 1.0, 1.0, 1.0, 1.0);

            TeamStrengthCalculator.PreMatchRates rates = TeamStrengthCalculator.calculatePreMatchRates(
                    home, away, 1.35, 1.25, neutralMods
            );

            assertThat(rates.effectiveHomeAdvantage()).isCloseTo(1.00, within(EPSILON));
            assertThat(rates.lambdaHome()).isCloseTo(1.35, within(EPSILON));
            assertThat(rates.muAway()).isCloseTo(1.35, within(EPSILON));
        }

        @Test
        @DisplayName("Tactical modifiers (attack/defense multipliers) should adjust rates directly")
        void shouldApplyTacticalMultipliers() {
            TeamStrengthCalculator.TeamStrength home = new TeamStrengthCalculator.TeamStrength(1.0, 1.0, 1.0, 1.0);
            TeamStrengthCalculator.TeamStrength away = new TeamStrengthCalculator.TeamStrength(1.0, 1.0, 1.0, 1.0);

            // Home attack boosted +20% (1.20), Away defense weakened (1.10) -> lambda * 1.20 * 1.10 = 1.32
            MatchModifiers mods = new MatchModifiers(true, false, false, false, false, 1.20, 1.00, 1.00, 1.10);

            TeamStrengthCalculator.PreMatchRates rates = TeamStrengthCalculator.calculatePreMatchRates(
                    home, away, 1.00, 1.20, mods
            );

            assertThat(rates.lambdaHome()).isCloseTo(1.32, within(EPSILON));
        }

        @Test
        @DisplayName("Mutual Low-Urgency should apply 0.65 factor pre-match")
        void shouldApplyMutualLowUrgencyPreMatch() {
            TeamStrengthCalculator.TeamStrength home = new TeamStrengthCalculator.TeamStrength(1.0, 1.0, 1.0, 1.0);
            TeamStrengthCalculator.TeamStrength away = new TeamStrengthCalculator.TeamStrength(1.0, 1.0, 1.0, 1.0);

            MatchModifiers mods = new MatchModifiers(true, false, false, true, true, 1.0, 1.0, 1.0, 1.0);

            TeamStrengthCalculator.PreMatchRates rates = TeamStrengthCalculator.calculatePreMatchRates(
                    home, away, 1.00, 1.20, mods
            );

            assertThat(rates.lambdaHome()).isCloseTo(0.65, within(EPSILON));
            assertThat(rates.muAway()).isCloseTo(0.65, within(EPSILON));
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class InvariantTests {

        @Test
        @DisplayName("Should throw on negative performance metrics")
        void shouldThrowOnNegativePerformance() {
            assertThatThrownBy(() -> new TeamStrengthCalculator.MatchPerformance(-1.0, 1.0, false))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on invalid league avg or home advantage bounds")
        void shouldThrowOnInvalidParameters() {
            assertThatThrownBy(() -> TeamStrengthCalculator.calculateStrength(List.of(), 0.0, 0.70))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> TeamStrengthCalculator.calculateStrength(List.of(), 1.35, 1.10))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> TeamStrengthCalculator.calculatePreMatchRates(null, null, 1.35, 0.90, null))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
