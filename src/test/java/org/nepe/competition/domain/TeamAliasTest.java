package org.nepe.competition.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TeamAlias Unit Tests")
class TeamAliasTest {

    @Nested
    @DisplayName("Creation and Mutation Tests")
    class CreationAndMutationTests {

        @Test
        @DisplayName("Should create alias with trimmed name and team ID")
        void shouldCreateAlias() {
            TeamAlias alias = TeamAlias.create("  Man City  ", 1);

            assertThat(alias.getId()).isNull();
            assertThat(alias.getAliasName()).isEqualTo("Man City");
            assertThat(alias.getTeamId()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reassign target team ID")
        void shouldReassignTeamId() {
            TeamAlias alias = TeamAlias.create("Spurs", 1);
            alias.reassignTeam(2);

            assertThat(alias.getTeamId()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should assign ID properly")
        void shouldAssignId() {
            TeamAlias alias = TeamAlias.create("Barca", 5);
            alias.assignId(10);

            assertThat(alias.getId()).isEqualTo(10);

            assertThatThrownBy(() -> alias.assignId(11))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw on blank alias name")
        void shouldThrowOnBlankAlias(String blank) {
            assertThatThrownBy(() -> TeamAlias.create(blank, 1))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on alias name exceeding 100 characters")
        void shouldThrowOnExcessiveAliasLength() {
            assertThatThrownBy(() -> TeamAlias.create("A".repeat(101), 1))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on non-positive teamId")
        void shouldThrowOnInvalidTeamId() {
            assertThatThrownBy(() -> TeamAlias.create("Inter", 0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> TeamAlias.create("Inter", -1))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> TeamAlias.create("Inter", null))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality and hashcode should be case-insensitive on alias name")
        void shouldBeEqualCaseInsensitive() {
            TeamAlias alias1 = new TeamAlias(1, "Man Utd", 1);
            TeamAlias alias2 = new TeamAlias(2, "man utd", 1);
            TeamAlias alias3 = new TeamAlias(1, "Man City", 2);

            assertThat(alias1).isEqualTo(alias2);
            assertThat(alias1.hashCode()).isEqualTo(alias2.hashCode());
            assertThat(alias1).isNotEqualTo(alias3);
        }
    }
}
