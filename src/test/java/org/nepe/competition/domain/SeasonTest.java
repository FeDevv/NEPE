package org.nepe.competition.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Season Unit Tests")
class SeasonTest {

    @Nested
    @DisplayName("Creation and Factory Method Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create valid season using year factory method")
        void shouldCreateFromStartYear() {
            Season season = Season.of(2025);

            assertThat(season.getId()).isNull();
            assertThat(season.getName()).isEqualTo("2025/2026");
            assertThat(season.getStartYear()).isEqualTo(2025);
            assertThat(season.getEndYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("Should create valid season from string")
        void shouldCreateFromString() {
            Season season = Season.create("2024/2025");

            assertThat(season.getName()).isEqualTo("2024/2025");
            assertThat(season.getStartYear()).isEqualTo(2024);
            assertThat(season.getEndYear()).isEqualTo(2025);
        }
    }

    @Nested
    @DisplayName("Navigation and Sequence Logic Tests")
    class NavigationTests {

        @Test
        @DisplayName("previous() should return predecessor season")
        void shouldReturnPreviousSeason() {
            Season season = Season.of(2025);
            Season prev = season.previous();

            assertThat(prev.getName()).isEqualTo("2024/2025");
            assertThat(prev.getStartYear()).isEqualTo(2024);
        }

        @Test
        @DisplayName("next() should return successor season")
        void shouldReturnNextSeason() {
            Season season = Season.of(2025);
            Season next = season.next();

            assertThat(next.getName()).isEqualTo("2026/2027");
            assertThat(next.getStartYear()).isEqualTo(2026);
        }

        @Test
        @DisplayName("isDirectPredecessorOf() should identify direct sequence")
        void shouldIdentifyDirectPredecessors() {
            Season s2024 = Season.of(2024);
            Season s2025 = Season.of(2025);
            Season s2026 = Season.of(2026);

            assertThat(s2024.isDirectPredecessorOf(s2025)).isTrue();
            assertThat(s2024.isDirectPredecessorOf(s2026)).isFalse();
            assertThat(s2025.isDirectPredecessorOf(s2024)).isFalse();
            assertThat(s2025.isDirectPredecessorOf(null)).isFalse();
        }

        @Test
        @DisplayName("Natural ordering should compare start years chronologically")
        void shouldCompareChronologically() {
            Season s2023 = Season.of(2023);
            Season s2024 = Season.of(2024);
            Season s2025 = Season.of(2025);

            assertThat(s2023.compareTo(s2024)).isNegative();
            assertThat(s2025.compareTo(s2024)).isPositive();
            assertThat(s2024.compareTo(Season.of(2024))).isZero();
            assertThat(s2024.compareTo(null)).isPositive();
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "   ",
                "2025",
                "2025-2026",
                "2025/2025",
                "2025/2027",
                "2026/2025",
                "abcd/efgh"
        })
        @DisplayName("Should throw exception on malformed season string")
        void shouldThrowOnMalformedSeason(String invalidFormat) {
            assertThatThrownBy(() -> Season.create(invalidFormat))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on start year out of bounds (< 1900 or >= 2100)")
        void shouldThrowOnYearOutOfBounds() {
            assertThatThrownBy(() -> Season.of(1899))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Season.of(2100))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should validate ID assignment")
        void shouldValidateIdAssignment() {
            Season season = Season.of(2025);
            assertThatThrownBy(() -> season.assignId(-1))
                    .isInstanceOf(DomainValidationException.class);

            season.assignId(1);
            assertThat(season.getId()).isEqualTo(1);

            assertThatThrownBy(() -> season.assignId(2))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality should be based on unique season name")
        void shouldBeEqualByName() {
            Season s1 = new Season(1, "2025/2026");
            Season s2 = new Season(2, "2025/2026");
            Season s3 = new Season(1, "2024/2025");

            assertThat(s1).isEqualTo(s2);
            assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
            assertThat(s1).isNotEqualTo(s3);
        }
    }
}
