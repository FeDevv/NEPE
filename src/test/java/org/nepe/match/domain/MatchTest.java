package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.LiveTradingException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Match Aggregate Root Unit Tests")
class MatchTest {

    private final Instant testKickoff = Instant.parse("2026-03-15T19:45:00Z");

    @Nested
    @DisplayName("Creation and Factory Tests")
    class CreationTests {

        @Test
        @DisplayName("createScheduled() should create valid scheduled match with default stats and modifiers")
        void shouldCreateScheduledMatch() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, 2.10, 3.40, 3.60);

            assertThat(match.getId()).isNull();
            assertThat(match.getSeasonId()).isEqualTo(1);
            assertThat(match.getCompetitionId()).isEqualTo(1);
            assertThat(match.getHomeTeamId()).isEqualTo(10);
            assertThat(match.getAwayTeamId()).isEqualTo(20);
            assertThat(match.getMatchDateTime()).isEqualTo(testKickoff);
            assertThat(match.getState()).isEqualTo(MatchState.SCHEDULED);
            assertThat(match.isScheduled()).isTrue();
            assertThat(match.isManuallyEdited()).isFalse();
            assertThat(match.getCurrentMinute()).isEqualTo(0);
            assertThat(match.hasReferenceOdds()).isTrue();
            assertThat(match.getOddsHome()).isEqualTo(2.10);
            assertThat(match.getOddsDraw()).isEqualTo(3.40);
            assertThat(match.getOddsAway()).isEqualTo(3.60);
            assertThat(match.getStatistics()).isNotNull();
            assertThat(match.getModifiers()).isNotNull();
        }
    }

    @Nested
    @DisplayName("State Transitions and Lifecycle Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Should transit from SCHEDULED to LIVE, then FINISHED")
        void shouldHandleStandardLifecycle() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);

            match.startLive();
            assertThat(match.isLive()).isTrue();
            assertThat(match.getState()).isEqualTo(MatchState.LIVE);

            match.updateCurrentMinute(45);
            assertThat(match.getCurrentMinute()).isEqualTo(45);

            match.finishMatch();
            assertThat(match.isFinished()).isTrue();
            assertThat(match.getState()).isEqualTo(MatchState.FINISHED);
            assertThat(match.getCurrentMinute()).isEqualTo(90); // Advances to 90 at finish if lower
        }

        @Test
        @DisplayName("Should handle postponement and rescheduling")
        void shouldHandlePostponeAndReschedule() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);

            match.postponeMatch();
            assertThat(match.getState()).isEqualTo(MatchState.POSTPONED);

            Instant newDate = testKickoff.plusSeconds(86400);
            match.reschedule(newDate);
            assertThat(match.getMatchDateTime()).isEqualTo(newDate);
            assertThat(match.isManuallyEdited()).isTrue();
        }

        @Test
        @DisplayName("Should prevent starting live tracking on terminal matches")
        void shouldPreventIllegalLiveStart() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);
            match.cancelMatch();

            assertThatThrownBy(match::startLive)
                    .isInstanceOf(LiveTradingException.class)
                    .hasMessageContaining("Cannot start live tracking for match in terminal state");
        }

        @Test
        @DisplayName("Should prevent updating minute when match is not LIVE")
        void shouldPreventMinuteUpdateWhenNotLive() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);

            assertThatThrownBy(() -> match.updateCurrentMinute(30))
                    .isInstanceOf(LiveTradingException.class)
                    .hasMessageContaining("Cannot update game minute when match is not LIVE");
        }
    }

    @Nested
    @DisplayName("Live Event Application and Rollback Tests")
    class LiveEventsTests {

        @Test
        @DisplayName("applyEvent should modify scoreline, cards, and advance current minute")
        void shouldApplyLiveEvents() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);
            match.startLive();

            MatchEvent homeGoal = MatchEvent.goalHome(1, 23);
            match.applyEvent(homeGoal);

            assertThat(match.getStatistics().getHomeScore()).isEqualTo(1);
            assertThat(match.getCurrentMinute()).isEqualTo(23);

            MatchEvent awayRedCard = MatchEvent.redCardAway(1, 55);
            match.applyEvent(awayRedCard);

            assertThat(match.getStatistics().getAwayRedCards()).isEqualTo(1);
            assertThat(match.getCurrentMinute()).isEqualTo(55);
        }

        @Test
        @DisplayName("revertEvent should decrement scoreline or cards accurately")
        void shouldRevertLiveEvents() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);
            match.startLive();

            MatchEvent homeGoal = MatchEvent.goalHome(1, 23);
            match.applyEvent(homeGoal);
            assertThat(match.getStatistics().getHomeScore()).isEqualTo(1);

            match.revertEvent(homeGoal);
            assertThat(match.getStatistics().getHomeScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should throw when applying event to finished or cancelled match")
        void shouldThrowWhenApplyingEventToTerminalMatch() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);
            match.cancelMatch();

            MatchEvent event = MatchEvent.goalHome(1, 10);
            assertThatThrownBy(() -> match.applyEvent(event))
                    .isInstanceOf(LiveTradingException.class);
        }
    }

    @Nested
    @DisplayName("Manual Edits & Overwrite Protection Tests")
    class OverwriteProtectionTests {

        @Test
        @DisplayName("Updating statistics or modifiers manually sets isManuallyEdited to true")
        void shouldFlagManualModifications() {
            Match match = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);
            assertThat(match.isManuallyEdited()).isFalse();

            match.updateModifiers(new MatchModifiers(true, false, false, false, false, 1.0, 1.0, 1.0, 1.0));
            assertThat(match.isManuallyEdited()).isTrue();

            Match match2 = Match.createScheduled(1, 1, 10, 20, testKickoff, null, null, null);
            match2.updateStatistics(new MatchStatistics(2, 0, 10, 5, 4, 1, 0, 0, null, null));
            assertThat(match2.isManuallyEdited()).isTrue();
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when Home team and Away team are the same")
        void shouldThrowWhenSameTeam() {
            assertThatThrownBy(() -> Match.createScheduled(1, 1, 10, 10, testKickoff, null, null, null))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Home team and Away team cannot be the same team");
        }

        @Test
        @DisplayName("Should throw on non-positive relational IDs or null date")
        void shouldThrowOnInvalidIdsOrDate() {
            assertThatThrownBy(() -> Match.createScheduled(0, 1, 10, 20, testKickoff, null, null, null))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> Match.createScheduled(1, 1, 10, 20, null, null, null, null))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on reference odds out of bounds (< 1.01 or > 1000)")
        void shouldThrowOnInvalidOdds() {
            assertThatThrownBy(() -> Match.createScheduled(1, 1, 10, 20, testKickoff, 0.95, 3.0, 3.0))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> Match.createScheduled(1, 1, 10, 20, testKickoff, 2.0, 1001.0, 3.0))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality without persisted ID should be based on (homeTeamId, awayTeamId, matchDateTime)")
        void shouldBeEqualOnBusinessKey() {
            Match m1 = Match.createScheduled(1, 1, 10, 20, testKickoff, 2.0, 3.0, 3.5);
            Match m2 = Match.createScheduled(1, 1, 10, 20, testKickoff, 2.1, 3.1, 3.6);
            Match m3 = Match.createScheduled(1, 1, 10, 21, testKickoff, 2.0, 3.0, 3.5);

            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
            assertThat(m1).isNotEqualTo(m3);
        }

        @Test
        @DisplayName("Equality with persisted ID should be based on ID")
        void shouldBeEqualOnPersistedId() {
            Match m1 = new Match(100, 1, 1, 10, 20, testKickoff, MatchState.SCHEDULED, false, null, null, null, null, null, 0);
            Match m2 = new Match(100, 1, 1, 15, 25, testKickoff.plusSeconds(3600), MatchState.LIVE, true, null, null, null, null, null, 10);

            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }
    }
}
