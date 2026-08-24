package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchEventType Unit Tests")
class MatchEventTypeTest {

    @Test
    @DisplayName("GOAL_HOME properties")
    void shouldVerifyGoalHome() {
        MatchEventType type = MatchEventType.GOAL_HOME;
        assertThat(type.isGoal()).isTrue();
        assertThat(type.isRedCard()).isFalse();
        assertThat(type.isHomeTeamEvent()).isTrue();
        assertThat(type.isAwayTeamEvent()).isFalse();
    }

    @Test
    @DisplayName("GOAL_AWAY properties")
    void shouldVerifyGoalAway() {
        MatchEventType type = MatchEventType.GOAL_AWAY;
        assertThat(type.isGoal()).isTrue();
        assertThat(type.isRedCard()).isFalse();
        assertThat(type.isHomeTeamEvent()).isFalse();
        assertThat(type.isAwayTeamEvent()).isTrue();
    }

    @Test
    @DisplayName("RED_CARD_HOME properties")
    void shouldVerifyRedCardHome() {
        MatchEventType type = MatchEventType.RED_CARD_HOME;
        assertThat(type.isGoal()).isFalse();
        assertThat(type.isRedCard()).isTrue();
        assertThat(type.isHomeTeamEvent()).isTrue();
        assertThat(type.isAwayTeamEvent()).isFalse();
    }

    @Test
    @DisplayName("RED_CARD_AWAY properties")
    void shouldVerifyRedCardAway() {
        MatchEventType type = MatchEventType.RED_CARD_AWAY;
        assertThat(type.isGoal()).isFalse();
        assertThat(type.isRedCard()).isTrue();
        assertThat(type.isHomeTeamEvent()).isFalse();
        assertThat(type.isAwayTeamEvent()).isTrue();
    }

    @Test
    @DisplayName("fromString should parse valid string representations")
    void shouldParseValidStrings() {
        assertThat(MatchEventType.fromString("goal_home")).isEqualTo(MatchEventType.GOAL_HOME);
        assertThat(MatchEventType.fromString("  RED_CARD_AWAY  ")).isEqualTo(MatchEventType.RED_CARD_AWAY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "YELLOW_CARD", "PENALTY"})
    @DisplayName("fromString should throw on invalid string")
    void shouldThrowOnInvalidStrings(String invalid) {
        assertThatThrownBy(() -> MatchEventType.fromString(invalid))
                .isInstanceOf(DomainValidationException.class);
    }
}
