package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("MatchStatistics Unit Tests")
class MatchStatisticsTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Creation and Effective xG Tests")
    class CreationAndXgTests {

        @Test
        @DisplayName("empty() statistics should return empty effective xG")
        void shouldCreateEmptyStatistics() {
            MatchStatistics stats = MatchStatistics.empty();

            assertThat(stats.getHomeScore()).isNull();
            assertThat(stats.getAwayScore()).isNull();
            assertThat(stats.getHomeRedCards()).isEqualTo(0);
            assertThat(stats.getAwayRedCards()).isEqualTo(0);
            assertThat(stats.getEffectiveHomeXg()).isEmpty();
            assertThat(stats.getEffectiveAwayXg()).isEmpty();
        }

        @Test
        @DisplayName("Effective xG should prioritize: manual override > shots heuristic > actual goals")
        void shouldResolveEffectiveXgPriority() {
            // Case 1: Manual override present
            MatchStatistics stats1 = new MatchStatistics(2, 1, 10, 8, 4, 3, 0, 0, 2.50, 1.20);
            assertThat(stats1.getEffectiveHomeXg()).hasValue(2.50);
            assertThat(stats1.getEffectiveAwayXg()).hasValue(1.20);

            // Case 2: No manual override -> shots heuristic: 4 * 0.30 + 6 * 0.05 = 1.50
            MatchStatistics stats2 = new MatchStatistics(2, 1, 10, 8, 4, 3, 0, 0, null, null);
            assertThat(stats2.getEffectiveHomeXg().getAsDouble()).isCloseTo(1.50, within(EPSILON));

            // Case 3: No shots -> fallback to actual goals
            MatchStatistics stats3 = new MatchStatistics(2, 1, null, null, null, null, 0, 0, null, null);
            assertThat(stats3.getEffectiveHomeXg()).hasValue(2.00);
            assertThat(stats3.getEffectiveAwayXg()).hasValue(1.00);
        }
    }

    @Nested
    @DisplayName("State Mutations Tests (Increments & Decrements)")
    class MutationTests {

        @Test
        @DisplayName("Should increment and decrement scoreline and red cards accurately")
        void shouldMutateScoresAndCards() {
            MatchStatistics stats = MatchStatistics.empty();

            // Increments
            stats.incrementHomeScore();
            stats.incrementHomeScore();
            stats.incrementAwayScore();
            stats.incrementHomeRedCards();
            stats.incrementAwayRedCards();

            assertThat(stats.getHomeScore()).isEqualTo(2);
            assertThat(stats.getAwayScore()).isEqualTo(1);
            assertThat(stats.getHomeRedCards()).isEqualTo(1);
            assertThat(stats.getAwayRedCards()).isEqualTo(1);

            // Decrements (reverting events)
            stats.decrementHomeScore();
            stats.decrementHomeRedCards();

            assertThat(stats.getHomeScore()).isEqualTo(1);
            assertThat(stats.getHomeRedCards()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should update scores, shots, and manual xG")
        void shouldUpdateStatisticsAttributes() {
            MatchStatistics stats = MatchStatistics.empty();

            stats.updateScores(3, 2);
            assertThat(stats.getHomeScore()).isEqualTo(3);
            assertThat(stats.getAwayScore()).isEqualTo(2);

            stats.updateShots(12, 10, 5, 4);
            assertThat(stats.getHomeShots()).isEqualTo(12);
            assertThat(stats.getHomeShotsOnTarget()).isEqualTo(5);

            stats.updateManualXg(2.85, 1.95);
            assertThat(stats.getManualHomeXg()).isEqualTo(2.85);
            assertThat(stats.getManualAwayXg()).isEqualTo(1.95);
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw on negative scores")
        void shouldThrowOnNegativeScore() {
            assertThatThrownBy(() -> new MatchStatistics(-1, 0, null, null, null, null, 0, 0, null, null))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> new MatchStatistics(0, -1, null, null, null, null, 0, 0, null, null))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw when shots on target exceed total shots")
        void shouldThrowWhenShotsOnTargetExceedTotal() {
            assertThatThrownBy(() -> new MatchStatistics(null, null, 5, 8, 6, 4, 0, 0, null, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot exceed total shots");
        }

        @Test
        @DisplayName("Should throw on negative red cards or invalid manual xG")
        void shouldThrowOnInvalidCardsOrXg() {
            assertThatThrownBy(() -> new MatchStatistics(null, null, null, null, null, null, -1, 0, null, null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new MatchStatistics(null, null, null, null, null, null, 0, 0, -0.5, null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new MatchStatistics(null, null, null, null, null, null, 0, 0, Double.NaN, null))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
