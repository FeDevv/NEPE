package org.nepe.competition.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Team Unit Tests")
class TeamTest {

    @Nested
    @DisplayName("Creation and Mutation Tests")
    class CreationAndMutationTests {

        @Test
        @DisplayName("Should create team with trimmed name")
        void shouldCreateTeam() {
            Team team = Team.create("  Juventus  ");

            assertThat(team.getId()).isNull();
            assertThat(team.getName()).isEqualTo("Juventus");
        }

        @Test
        @DisplayName("Should rename team")
        void shouldRenameTeam() {
            Team team = Team.create("Inter");
            team.rename("Inter Milan");

            assertThat(team.getName()).isEqualTo("Inter Milan");
        }

        @Test
        @DisplayName("Should assign ID properly")
        void shouldAssignId() {
            Team team = Team.create("Milan");
            team.assignId(5);

            assertThat(team.getId()).isEqualTo(5);

            // Reassignment of different ID throws exception
            assertThatThrownBy(() -> team.assignId(6))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw on blank name")
        void shouldThrowOnBlankName(String blank) {
            assertThatThrownBy(() -> Team.create(blank))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on name exceeding 100 characters")
        void shouldThrowOnExcessiveNameLength() {
            assertThatThrownBy(() -> Team.create("A".repeat(101)))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on invalid ID")
        void shouldThrowOnInvalidId() {
            Team team = Team.create("Roma");
            assertThatThrownBy(() -> team.assignId(0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> team.assignId(-1))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality and hashcode should be case-insensitive on team name")
        void shouldBeEqualCaseInsensitive() {
            Team team1 = new Team(1, "Arsenal");
            Team team2 = new Team(2, "arsenal");
            Team team3 = new Team(1, "Chelsea");

            assertThat(team1).isEqualTo(team2);
            assertThat(team1.hashCode()).isEqualTo(team2.hashCode());
            assertThat(team1).isNotEqualTo(team3);
        }
    }
}
