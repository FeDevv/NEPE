package org.nepe.competition.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.competition.port.in.ManageTeamUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("AliasMappingController Unit Tests")
class AliasMappingControllerTest {

    private final ManageTeamUseCase manageTeamUseCase = mock(ManageTeamUseCase.class);

    @Test
    @DisplayName("Constructor should reject null ManageTeamUseCase")
    void shouldRejectNullManageTeamUseCase() {
        assertThatThrownBy(() -> new AliasMappingController(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageTeamUseCase must not be null");
    }

    @Test
    @DisplayName("AliasMappingController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        AliasMappingController controller = new AliasMappingController(manageTeamUseCase);

        assertThat(controller).isNotNull();
        assertThat(controller.isResolved()).isFalse();
    }
}
