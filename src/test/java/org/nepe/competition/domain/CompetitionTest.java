package org.nepe.competition.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Competition Unit Tests")
class CompetitionTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("Creation and Factory Method Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create competition with default Dixon-Coles rho")
        void shouldCreateWithDefaultRho() {
            Competition comp = Competition.create("I1", "Serie A", "Italy");

            assertThat(comp.getId()).isNull();
            assertThat(comp.getCode()).isEqualTo("I1");
            assertThat(comp.getName()).isEqualTo("Serie A");
            assertThat(comp.getCountry()).isEqualTo("Italy");
            assertThat(comp.getDixonColesRho()).isCloseTo(Competition.DEFAULT_DIXON_COLES_RHO, within(EPSILON));
        }

        @Test
        @DisplayName("Should create competition with custom Dixon-Coles rho")
        void shouldCreateWithCustomRho() {
            Competition comp = Competition.create("E0", "Premier League", "England", -0.1050);

            assertThat(comp.getCode()).isEqualTo("E0");
            assertThat(comp.getName()).isEqualTo("Premier League");
            assertThat(comp.getCountry()).isEqualTo("England");
            assertThat(comp.getDixonColesRho()).isCloseTo(-0.1050, within(EPSILON));
        }

        @Test
        @DisplayName("Should normalize whitespace and code casing")
        void shouldNormalizeStrings() {
            Competition comp = Competition.create("  d1  ", "  Bundesliga  ", "  Germany  ");

            assertThat(comp.getCode()).isEqualTo("D1");
            assertThat(comp.getName()).isEqualTo("Bundesliga");
            assertThat(comp.getCountry()).isEqualTo("Germany");
        }
    }

    @Nested
    @DisplayName("Mutation and State Update Tests")
    class MutationTests {

        @Test
        @DisplayName("Should update details and Dixon-Coles rho")
        void shouldUpdateDetailsAndRho() {
            Competition comp = Competition.create("I1", "Serie A", "Italy");

            comp.updateDetails("Serie A TIM", "Italia");
            comp.updateDixonColesRho(-0.1350);

            assertThat(comp.getName()).isEqualTo("Serie A TIM");
            assertThat(comp.getCountry()).isEqualTo("Italia");
            assertThat(comp.getDixonColesRho()).isCloseTo(-0.1350, within(EPSILON));
        }

        @Test
        @DisplayName("Should assign ID once and prevent reassignment")
        void shouldAssignIdProperly() {
            Competition comp = Competition.create("I1", "Serie A", "Italy");

            comp.assignId(10);
            assertThat(comp.getId()).isEqualTo(10);

            // Reassigning same ID is idempotent
            comp.assignId(10);
            assertThat(comp.getId()).isEqualTo(10);

            // Reassigning different ID throws exception
            assertThatThrownBy(() -> comp.assignId(20))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Cannot reassign");
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should throw on blank code, name, or country")
        void shouldThrowOnBlankFields(String blank) {
            assertThatThrownBy(() -> Competition.create(blank, "Serie A", "Italy"))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Competition.create("I1", blank, "Italy"))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Competition.create("I1", "Serie A", blank))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw on excessive field lengths")
        void shouldThrowOnExcessiveLength() {
            assertThatThrownBy(() -> Competition.create("A".repeat(11), "Serie A", "Italy"))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Competition.create("I1", "A".repeat(101), "Italy"))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Competition.create("I1", "Serie A", "A".repeat(51)))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw when rho is outside (-1.0, 1.0) or non-finite")
        void shouldThrowOnInvalidRho() {
            assertThatThrownBy(() -> Competition.create("I1", "Serie A", "Italy", -1.0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Competition.create("I1", "Serie A", "Italy", 1.0))
                    .isInstanceOf(DomainValidationException.class);
            assertThatThrownBy(() -> Competition.create("I1", "Serie A", "Italy", Double.NaN))
                    .isInstanceOf(DomainValidationException.class);
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality and hashcode should be based on unique business code")
        void shouldBeEqualBasedOnCode() {
            Competition comp1 = new Competition(1, "I1", "Serie A", "Italy", -0.12);
            Competition comp2 = new Competition(2, "I1", "Italian Serie A", "Italy", -0.15);
            Competition comp3 = new Competition(1, "E0", "Premier League", "England", -0.12);

            assertThat(comp1).isEqualTo(comp2);
            assertThat(comp1.hashCode()).isEqualTo(comp2.hashCode());
            assertThat(comp1).isNotEqualTo(comp3);
        }
    }
}
