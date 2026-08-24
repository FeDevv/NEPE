package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketType Unit Tests")
class MarketTypeTest {

    @Test
    @DisplayName("MATCH_ODDS should have 1, X, 2 outcomes and no threshold")
    void shouldVerifyMatchOdds() {
        MarketType type = MarketType.MATCH_ODDS;
        assertThat(type.isUnderOver()).isFalse();
        assertThat(type.getThreshold()).isEmpty();
        assertThat(type.getValidOutcomes()).containsExactlyInAnyOrder("1", "X", "2");

        assertThat(type.isValidOutcome("1")).isTrue();
        assertThat(type.isValidOutcome("x")).isTrue(); // Case insensitive
        assertThat(type.isValidOutcome("2")).isTrue();
        assertThat(type.isValidOutcome("OVER")).isFalse();
    }

    @Test
    @DisplayName("UNDER_OVER markets should have OVER, UNDER outcomes and exact threshold")
    void shouldVerifyUnderOverMarkets() {
        MarketType uo25 = MarketType.UNDER_OVER_25;
        assertThat(uo25.isUnderOver()).isTrue();
        assertThat(uo25.getThreshold()).hasValue(2.5);
        assertThat(uo25.getValidOutcomes()).containsExactlyInAnyOrder("UNDER", "OVER");

        assertThat(uo25.isValidOutcome("OVER")).isTrue();
        assertThat(uo25.isValidOutcome("under")).isTrue();
        assertThat(uo25.isValidOutcome("1")).isFalse();
    }

    @Test
    @DisplayName("BTTS should have YES, NO outcomes")
    void shouldVerifyBtts() {
        MarketType btts = MarketType.BTTS;
        assertThat(btts.isUnderOver()).isFalse();
        assertThat(btts.getValidOutcomes()).containsExactlyInAnyOrder("YES", "NO");

        assertThat(btts.isValidOutcome("YES")).isTrue();
        assertThat(btts.isValidOutcome("no")).isTrue();
        assertThat(btts.isValidOutcome("OVER")).isFalse();
    }

    @Test
    @DisplayName("fromString should parse valid string representations ignoring case and spaces")
    void shouldParseValidStrings() {
        assertThat(MarketType.fromString("match_odds")).isEqualTo(MarketType.MATCH_ODDS);
        assertThat(MarketType.fromString("  UNDER_OVER_25  ")).isEqualTo(MarketType.UNDER_OVER_25);
        assertThat(MarketType.fromString("BTTS")).isEqualTo(MarketType.BTTS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "UNKNOWN_MARKET", "HANDICAP"})
    @DisplayName("fromString should throw on invalid strings")
    void shouldThrowOnInvalidStrings(String invalid) {
        assertThatThrownBy(() -> MarketType.fromString(invalid))
                .isInstanceOf(DomainValidationException.class);
    }
}
