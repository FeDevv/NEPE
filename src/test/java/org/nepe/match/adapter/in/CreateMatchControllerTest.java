package org.nepe.match.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.competition.port.in.ManageCompetitionUseCase;
import org.nepe.competition.port.in.ManageSeasonUseCase;
import org.nepe.competition.port.in.ManageTeamUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("CreateMatchController Unit Tests")
class CreateMatchControllerTest {

    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final ManageCompetitionUseCase manageCompetitionUseCase = mock(ManageCompetitionUseCase.class);
    private final ManageSeasonUseCase manageSeasonUseCase = mock(ManageSeasonUseCase.class);
    private final ManageTeamUseCase manageTeamUseCase = mock(ManageTeamUseCase.class);

    @Test
    @DisplayName("Constructor should reject null ManageMatchUseCase")
    void shouldRejectNullManageMatchUseCase() {
        assertThatThrownBy(() -> new CreateMatchController(null, manageCompetitionUseCase, manageSeasonUseCase, manageTeamUseCase))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");
    }

    @Test
    @DisplayName("Constructor should reject null ManageCompetitionUseCase")
    void shouldRejectNullManageCompetitionUseCase() {
        assertThatThrownBy(() -> new CreateMatchController(manageMatchUseCase, null, manageSeasonUseCase, manageTeamUseCase))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageCompetitionUseCase must not be null");
    }

    @Test
    @DisplayName("Constructor should reject null ManageSeasonUseCase")
    void shouldRejectNullManageSeasonUseCase() {
        assertThatThrownBy(() -> new CreateMatchController(manageMatchUseCase, manageCompetitionUseCase, null, manageTeamUseCase))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSeasonUseCase must not be null");
    }

    @Test
    @DisplayName("Constructor should reject null ManageTeamUseCase")
    void shouldRejectNullManageTeamUseCase() {
        assertThatThrownBy(() -> new CreateMatchController(manageMatchUseCase, manageCompetitionUseCase, manageSeasonUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageTeamUseCase must not be null");
    }

    @Test
    @DisplayName("CreateMatchController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        CreateMatchController controller = new CreateMatchController(
                manageMatchUseCase,
                manageCompetitionUseCase,
                manageSeasonUseCase,
                manageTeamUseCase
        );

        assertThat(controller).isNotNull();
        assertThat(controller.isMatchCreated()).isFalse();
    }
}
