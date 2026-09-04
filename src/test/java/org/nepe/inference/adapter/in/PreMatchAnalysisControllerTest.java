package org.nepe.inference.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.settings.port.in.ManageSettingsUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("PreMatchAnalysisController Unit Tests")
class PreMatchAnalysisControllerTest {

    private final CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase = mock(CalculatePreMatchInferenceUseCase.class);
    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final ManageMarketOddsUseCase manageMarketOddsUseCase = mock(ManageMarketOddsUseCase.class);
    private final ManageSettingsUseCase manageSettingsUseCase = mock(ManageSettingsUseCase.class);
    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new PreMatchAnalysisController(
                null, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CalculatePreMatchInferenceUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, null, manageMarketOddsUseCase, manageSettingsUseCase, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, null, manageSettingsUseCase, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMarketOddsUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, null, manageCompetitionUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSettingsUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");

        assertThatThrownBy(() -> new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, manageCompetitionUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("PreMatchAnalysisController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        PreMatchAnalysisController controller = new PreMatchAnalysisController(
                calculatePreMatchInferenceUseCase,
                manageMatchUseCase,
                manageMarketOddsUseCase,
                manageSettingsUseCase,
                manageCompetitionUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
        assertThat(controller.getScopeCompetitionId()).isNull();
        assertThat(controller.getScopeSeasonId()).isNull();
    }
}
