package org.nepe.match.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MatchModifiers Unit Tests")
class MatchModifiersTest {

    @Nested
    @DisplayName("Creation and Defaults Tests")
    class CreationTests {

        @Test
        @DisplayName("defaultModifiers() should have all flags false and 1.00 multipliers")
        void shouldCreateDefaultModifiers() {
            MatchModifiers mods = MatchModifiers.defaultModifiers();

            assertThat(mods.isNeutralVenue()).isFalse();
            assertThat(mods.isMustWinHome()).isFalse();
            assertThat(mods.isMustWinAway()).isFalse();
            assertThat(mods.isLowUrgencyHome()).isFalse();
            assertThat(mods.isLowUrgencyAway()).isFalse();

            assertThat(mods.getModAttHome()).isEqualTo(1.00);
            assertThat(mods.getModDefHome()).isEqualTo(1.00);
            assertThat(mods.getModAttAway()).isEqualTo(1.00);
            assertThat(mods.getModDefAway()).isEqualTo(1.00);

            assertThat(mods.hasCustomModifiers()).isFalse();
            assertThat(mods.isMutualLowUrgency()).isFalse();
        }

        @Test
        @DisplayName("hasCustomModifiers() should return true when any modifier is altered")
        void shouldDetectCustomModifiers() {
            MatchModifiers mods1 = new MatchModifiers(true, false, false, false, false, 1.0, 1.0, 1.0, 1.0);
            assertThat(mods1.hasCustomModifiers()).isTrue();

            MatchModifiers mods2 = new MatchModifiers(false, false, false, false, false, 1.1, 1.0, 1.0, 1.0);
            assertThat(mods2.hasCustomModifiers()).isTrue();
        }

        @Test
        @DisplayName("isMutualLowUrgency() should return true only when both home and away have low urgency")
        void shouldDetectMutualLowUrgency() {
            MatchModifiers mutual = new MatchModifiers(false, false, false, true, true, 1.0, 1.0, 1.0, 1.0);
            assertThat(mutual.isMutualLowUrgency()).isTrue();

            MatchModifiers single = new MatchModifiers(false, false, false, true, false, 1.0, 1.0, 1.0, 1.0);
            assertThat(single.isMutualLowUrgency()).isFalse();
        }
    }

    @Nested
    @DisplayName("Invariant Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw when multiplier is outside [0.10, 3.00] or non-finite")
        void shouldThrowOnInvalidMultiplier() {
            assertThatThrownBy(() -> new MatchModifiers(false, false, false, false, false, 0.05, 1.0, 1.0, 1.0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("must be between");

            assertThatThrownBy(() -> new MatchModifiers(false, false, false, false, false, 3.05, 1.0, 1.0, 1.0))
                    .isInstanceOf(DomainValidationException.class);

            assertThatThrownBy(() -> new MatchModifiers(false, false, false, false, false, Double.NaN, 1.0, 1.0, 1.0))
                    .isInstanceOf(DomainValidationException.class);
        }

        @Test
        @DisplayName("Should throw contradiction exception if a team is simultaneously Must-Win and Low-Urgency")
        void shouldThrowOnContradictoryFlags() {
            // Home side contradiction
            assertThatThrownBy(() -> new MatchModifiers(false, true, false, true, false, 1.0, 1.0, 1.0, 1.0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Home team cannot be simultaneously marked as Must-Win and Low-Urgency");

            // Away side contradiction
            assertThatThrownBy(() -> new MatchModifiers(false, false, true, false, true, 1.0, 1.0, 1.0, 1.0))
                    .isInstanceOf(DomainValidationException.class)
                    .hasMessageContaining("Away team cannot be simultaneously marked as Must-Win and Low-Urgency");
        }
    }

    @Nested
    @DisplayName("Identity and Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Equality and hashcode should match all fields")
        void shouldTestEquality() {
            MatchModifiers m1 = new MatchModifiers(false, true, false, false, false, 1.2, 1.0, 1.0, 1.0);
            MatchModifiers m2 = new MatchModifiers(false, true, false, false, false, 1.2, 1.0, 1.0, 1.0);
            MatchModifiers m3 = new MatchModifiers(false, false, false, false, false, 1.2, 1.0, 1.0, 1.0);

            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
            assertThat(m1).isNotEqualTo(m3);
        }
    }
}
