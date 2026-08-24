package org.nepe.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NepeException Hierarchy Unit Tests")
class NepeExceptionTest {

    @Test
    @DisplayName("DomainValidationException should extend NepeException and hold message/cause")
    void shouldVerifyDomainValidationException() {
        IllegalArgumentException cause = new IllegalArgumentException("invalid arg");
        DomainValidationException ex = new DomainValidationException("Validation failed", cause);

        assertThat(ex).isInstanceOf(NepeException.class);
        assertThat(ex.getMessage()).isEqualTo("Validation failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("EntityNotFoundException should construct structured missing entity message")
    void shouldVerifyEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Team", 42);

        assertThat(ex).isInstanceOf(NepeException.class);
        assertThat(ex.getMessage()).isEqualTo("Team with identifier '42' was not found.");
    }

    @Test
    @DisplayName("DataImportException should hold line number context")
    void shouldVerifyDataImportException() {
        DataImportException ex = new DataImportException("Invalid date format", 15);

        assertThat(ex).isInstanceOf(NepeException.class);
        assertThat(ex.getLineNumber()).isEqualTo(15);
        assertThat(ex.getMessage()).contains("Error at line 15: Invalid date format");
    }

    @Test
    @DisplayName("AliasMappingRequiredException should hold raw team name and competition code")
    void shouldVerifyAliasMappingRequiredException() {
        AliasMappingRequiredException ex = new AliasMappingRequiredException("Man City", "E0");

        assertThat(ex).isInstanceOf(NepeException.class);
        assertThat(ex.getRawTeamName()).isEqualTo("Man City");
        assertThat(ex.getCompetitionCode()).isEqualTo("E0");
        assertThat(ex.getMessage()).contains("Man City").contains("E0");
    }

    @Test
    @DisplayName("LiveTradingException should extend NepeException")
    void shouldVerifyLiveTradingException() {
        LiveTradingException ex = new LiveTradingException("Match already completed");

        assertThat(ex).isInstanceOf(NepeException.class);
        assertThat(ex.getMessage()).isEqualTo("Match already completed");
    }
}
