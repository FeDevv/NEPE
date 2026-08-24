package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("MarketOdds Unit Tests")
class MarketOddsTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Creation and Factory Methods Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create two-sided MarketOdds with uppercase normalized outcome")
        void shouldCreateTwoSidedOdds() {
            MarketOdds odds = MarketOdds.create(1, MarketType.MATCH_ODDS, "1", 2.10, 2.14);

            assertThat(odds.getId()).isNull();
            assertThat(odds.getMatchId()).isEqualTo(1);
            assertThat(odds.getMarketType()).isEqualTo(MarketType.MATCH_ODDS);
            assertThat(odds.getOutcome()).isEqualTo("1");
            assertThat(odds.getBackOdds()).isEqualTo(2.10);
            assertThat(odds.getLayOdds()).isEqualTo(2.14);
            assertThat(odds.hasBothOdds()).isTrue();
            assertThat(odds.getSpread()).isPresent();
            assertThat(odds.getSpread().getAsDouble()).isCloseTo(0.04, within(EPSILON));
        }

        @Test
        @DisplayName("Should create Back-only and Lay-only MarketOdds")
        void shouldCreateOneSidedOdds() {
            MarketOdds backOnly = MarketOdds.backOnly(1, MarketType.UNDER_OVER_25, "OVER", 1.95);
            assertThat(backOnly.hasBackOdds()).isTrue();
            assertThat(backOnly.hasLayOdds()).isFalse();
            assertThat(backOnly.hasBothOdds()).isFalse();
            assertThat(backOnly.getSpread()).isEmpty();

            MarketOdds layOnly = MarketOdds.layOnly(1, MarketType.UNDER_OVER_25, "OVER", 2.05);
            assertThat(layOnly.hasBackOdds()).isFalse();
            assertThat(layOnly.hasLayOdds()).isTrue();
        }
    }

    @Nested
    @DisplayName("Mutation Tests")
    class MutationTests {

        @Test
        @DisplayName("Should update odds and spread dynamically")
        void shouldUpdateOdds() {
            MarketOdds odds = MarketOdds.backOnly(1, MarketType.BTTS, "YES", 1.80);

            odds.updateOdds(1.85, 1.90);
            assertThat(odds.getBackOdds()).isEqualTo(1.85);
            assertThat(odds.getLayOdds()).isEqualTo(1.90);
            assertThat(odds.getSpread()).isPresent();
            assertThat(odds.getSpread().getAsDouble()).isCloseTo(0.05, within(EPSILON));

            odds.updateBackOdds(1.86);
            assertThat(odds.getBackOdds()).isEqualTo(1.86);

            odds.updateLayOdds(1.92);
            assertThat(odds.getLayOdds()).isEqualTo(1.92);
        }

        @Test
        @DisplayName("Should assign ID properly")
        void shouldAssignId() {
            MarketOdds odds = MarketOdds.backOnly(1, MarketType.MATCH_ODDS, "X", 3.40);
            odds.assignId(100);
            assertThat(odds.getId()).isEqualTo(100);

            assertThatThrownBy(() -> odds.assignId(101))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw on non-positive matchId")
        void shouldThrowOnInvalidMatchId() {
            assertThatThrownBy(() -> MarketOdds.create(0, MarketType.MATCH_ODDS, "1", 2.0, 2.1))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> MarketOdds.create(null, MarketType.MATCH_ODDS, "1", 2.0, 2.1))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on invalid outcome for the specific market type")
        void shouldThrowOnInvalidOutcome() {
            // MATCH_ODDS does not accept "OVER"
            assertThatThrownBy(() -> MarketOdds.create(1, MarketType.MATCH_ODDS, "OVER", 2.0, 2.1))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Invalid outcome");
        }

        @Test
        @DisplayName("Should throw when both Back and Lay odds are null")
        void shouldThrowWhenBothOddsNull() {
            assertThatThrownBy(() -> MarketOdds.create(1, MarketType.MATCH_ODDS, "1", null, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("At least one of Back odds or Lay odds must be provided");
        }

        @Test
        @DisplayName("Should throw when Lay odds is strictly lower than Back odds")
        void shouldThrowWhenLayLowerThanBack() {
            assertThatThrownBy(() -> MarketOdds.create(1, MarketType.MATCH_ODDS, "1", 2.10, 2.05))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("cannot be strictly lower than Back odds");
        }

        @Test
        @DisplayName("Should throw when odds are out of valid range (1.01 to 1000.0)")
        void shouldThrowOnOddsOutOfRange() {
            assertThatThrownBy(() -> MarketOdds.backOnly(1, MarketType.MATCH_ODDS, "1", 1.00))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> MarketOdds.backOnly(1, MarketType.MATCH_ODDS, "1", 1001.0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> MarketOdds.backOnly(1, MarketType.MATCH_ODDS, "1", Double.NaN))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality should be based on composite key (matchId, marketType, outcome)")
        void shouldBeEqualOnCompositeKey() {
            MarketOdds o1 = new MarketOdds(1, 10, MarketType.MATCH_ODDS, "1", 2.00, 2.05);
            MarketOdds o2 = new MarketOdds(2, 10, MarketType.MATCH_ODDS, "1", 2.10, 2.15);
            MarketOdds o3 = new MarketOdds(1, 10, MarketType.MATCH_ODDS, "X", 3.00, 3.10);

            assertThat(o1).isEqualTo(o2);
            assertThat(o1.hashCode()).isEqualTo(o2.hashCode());
            assertThat(o1).isNotEqualTo(o3);
        }
    }
}
