package org.nepe.competition.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("CompetitionViewController Unit Tests")
class CompetitionViewControllerTest {

    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
    private final ManageTeamUseCase manageTeamUseCase = mock(ManageTeamUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new CompetitionViewController(null, manageTeamUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");

        assertThatThrownBy(() -> new CompetitionViewController(manageCompetitionUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageTeamUseCase must not be null");

        assertThatThrownBy(() -> new CompetitionViewController(manageCompetitionUseCase, manageTeamUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("CompetitionViewController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        CompetitionViewController controller = new CompetitionViewController(
                manageCompetitionUseCase,
                manageTeamUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
    }
}
