package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchEvent Unit Tests")
class MatchEventTest {

    @Nested
    @DisplayName("Creation and Factory Methods Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create event with factory methods")
        void shouldCreateWithFactory() {
            MatchEvent goal = MatchEvent.goalHome(10, 23);
            assertThat(goal.getMatchId()).isEqualTo(10);
            assertThat(goal.getEventType()).isEqualTo(MatchEventType.GOAL_HOME);
            assertThat(goal.getMinute()).isEqualTo(23);
            assertThat(goal.isGoal()).isTrue();
            assertThat(goal.isHomeTeamEvent()).isTrue();
            assertThat(goal.getCreatedAt()).isNotNull();

            MatchEvent redCard = MatchEvent.redCardAway(10, 75);
            assertThat(redCard.isRedCard()).isTrue();
            assertThat(redCard.isAwayTeamEvent()).isTrue();
        }

        @Test
        @DisplayName("Should assign ID properly")
        void shouldAssignId() {
            MatchEvent event = MatchEvent.goalAway(10, 44);
            event.assignId(1);
            assertThat(event.getId()).isEqualTo(1);

            assertThatThrownBy(() -> event.assignId(2))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Chronological Comparison Tests")
    class ComparisonTests {

        @Test
        @DisplayName("compareTo should order by minute and timestamp")
        void shouldOrderChronologically() {
            Instant now = Instant.now();
            MatchEvent e1 = new MatchEvent(1, 10, MatchEventType.GOAL_HOME, 15, now);
            MatchEvent e2 = new MatchEvent(2, 10, MatchEventType.GOAL_AWAY, 45, now.plusSeconds(10));
            MatchEvent e3 = new MatchEvent(3, 10, MatchEventType.RED_CARD_HOME, 45, now.plusSeconds(20));

            assertThat(e1.compareTo(e2)).isNegative();
            assertThat(e2.compareTo(e1)).isPositive();
            assertThat(e2.compareTo(e3)).isNegative(); // Same minute, earlier timestamp
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw on non-positive matchId")
        void shouldThrowOnInvalidMatchId() {
            assertThatThrownBy(() -> MatchEvent.create(0, MatchEventType.GOAL_HOME, 10))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> MatchEvent.create(null, MatchEventType.GOAL_HOME, 10))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on null event type")
        void shouldThrowOnNullEventType() {
            assertThatThrownBy(() -> MatchEvent.create(1, null, 10))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on minute out of bounds (< 0 or > 130)")
        void shouldThrowOnInvalidMinute() {
            assertThatThrownBy(() -> MatchEvent.create(1, MatchEventType.GOAL_HOME, -1))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> MatchEvent.create(1, MatchEventType.GOAL_HOME, 131))
                    .isInstanceOf(DomainValidationException.class);
        }
    }
}
