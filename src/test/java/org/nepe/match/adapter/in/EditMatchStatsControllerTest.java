package org.nepe.match.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.match.port.in.ManageMatchUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("EditMatchStatsController Unit Tests")
class EditMatchStatsControllerTest {

    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);

    @Test
    @DisplayName("Constructor should reject null ManageMatchUseCase")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new EditMatchStatsController(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");
    }

    @Test
    @DisplayName("EditMatchStatsController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        EditMatchStatsController controller = new EditMatchStatsController(manageMatchUseCase);

        assertThat(controller).isNotNull();
        assertThat(controller.isStatsUpdated()).isFalse();
    }
}
