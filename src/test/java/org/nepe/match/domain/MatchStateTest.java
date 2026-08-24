package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchState Unit Tests")
class MatchStateTest {

    @Test
    @DisplayName("SCHEDULED state properties")
    void shouldVerifyScheduled() {
        MatchState state = MatchState.SCHEDULED;
        assertThat(state.isScheduled()).isTrue();
        assertThat(state.isLive()).isFalse();
        assertThat(state.isFinished()).isFalse();
        assertThat(state.isTerminal()).isFalse();
        assertThat(state.allowsPreMatchAnalysis()).isTrue();
        assertThat(state.allowsLiveTrading()).isFalse();
    }

    @Test
    @DisplayName("LIVE state properties")
    void shouldVerifyLive() {
        MatchState state = MatchState.LIVE;
        assertThat(state.isScheduled()).isFalse();
        assertThat(state.isLive()).isTrue();
        assertThat(state.isTerminal()).isFalse();
        assertThat(state.allowsLiveTrading()).isTrue();
    }

    @Test
    @DisplayName("FINISHED state properties")
    void shouldVerifyFinished() {
        MatchState state = MatchState.FINISHED;
        assertThat(state.isFinished()).isTrue();
        assertThat(state.isTerminal()).isTrue();
        assertThat(state.allowsLiveTrading()).isFalse();
    }

    @Test
    @DisplayName("POSTPONED and CANCELLED states")
    void shouldVerifyPostponedAndCancelled() {
        MatchState postponed = MatchState.POSTPONED;
        assertThat(postponed.allowsPreMatchAnalysis()).isTrue();
        assertThat(postponed.isTerminal()).isFalse();

        MatchState cancelled = MatchState.CANCELLED;
        assertThat(cancelled.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("fromString should parse valid names")
    void shouldParseValidStrings() {
        assertThat(MatchState.fromString("scheduled")).isEqualTo(MatchState.SCHEDULED);
        assertThat(MatchState.fromString("LIVE")).isEqualTo(MatchState.LIVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "IN_PROGRESS", "OVER"})
    @DisplayName("fromString should throw on invalid names")
    void shouldThrowOnInvalidStrings(String invalid) {
        assertThatThrownBy(() -> MatchState.fromString(invalid))
                .isInstanceOf(DomainValidationException.class);
    }
}
