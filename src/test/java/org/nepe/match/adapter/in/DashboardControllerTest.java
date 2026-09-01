package org.nepe.match.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.inference.port.in.CalculatePreMatchInferenceUseCase;
import org.nepe.match.port.in.ImportCsvMatchesUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
    private final ManageSeasonUseCase manageSeasonUseCase = mock(ManageSeasonUseCase.class);
    private final ImportCsvMatchesUseCase importCsvMatchesUseCase = mock(ImportCsvMatchesUseCase.class);
    private final CalculatePreMatchInferenceUseCase calculatePreMatchInferenceUseCase = mock(CalculatePreMatchInferenceUseCase.class);
    private final org.nepe.settings.port.in.ManageSettingsUseCase manageSettingsUseCase = mock(org.nepe.settings.port.in.ManageSettingsUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @Test
    @DisplayName("Constructor should enforce non-null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new DashboardController(
                null, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, null, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, manageCompetitionUseCase, null, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSeasonUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, manageCompetitionUseCase, manageSeasonUseCase, null, calculatePreMatchInferenceUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ImportCsvMatchesUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, null, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CalculatePreMatchInferenceUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSettingsUseCase must not be null");

        assertThatThrownBy(() -> new DashboardController(
                manageMatchUseCase, manageCompetitionUseCase, manageSeasonUseCase, importCsvMatchesUseCase, calculatePreMatchInferenceUseCase, manageSettingsUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("DashboardController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        DashboardController controller = new DashboardController(
                manageMatchUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                importCsvMatchesUseCase,
                calculatePreMatchInferenceUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
        assertThat(controller.getSelectedMatchIdForNavigation()).isNull();
    }
}
