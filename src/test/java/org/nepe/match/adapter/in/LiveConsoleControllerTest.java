package org.nepe.match.adapter.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.bootstrap.SpringFXMLLoader;
import org.nepe.inference.port.in.CalculateLiveInferenceUseCase;
import org.nepe.match.port.in.LiveMatchTradingUseCase;
import org.nepe.match.port.in.ManageMarketOddsUseCase;
import org.nepe.match.port.in.ManageMatchUseCase;
import org.nepe.settings.port.in.ManageSettingsUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("LiveConsoleController Unit Tests")
class LiveConsoleControllerTest {

    private final LiveMatchTradingUseCase liveMatchTradingUseCase = mock(LiveMatchTradingUseCase.class);
    private final CalculateLiveInferenceUseCase calculateLiveInferenceUseCase = mock(CalculateLiveInferenceUseCase.class);
    private final ManageMatchUseCase manageMatchUseCase = mock(ManageMatchUseCase.class);
    private final ManageMarketOddsUseCase manageMarketOddsUseCase = mock(ManageMarketOddsUseCase.class);
    private final ManageSettingsUseCase manageSettingsUseCase = mock(ManageSettingsUseCase.class);
    private final SpringFXMLLoader springFXMLLoader = mock(SpringFXMLLoader.class);

    @Test
    @DisplayName("Constructor should reject null dependencies")
    void shouldEnforceConstructorInvariants() {
        assertThatThrownBy(() -> new LiveConsoleController(
                null, calculateLiveInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("LiveMatchTradingUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, null, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CalculateLiveInferenceUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, null, manageMarketOddsUseCase, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMatchUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, manageMatchUseCase, null, manageSettingsUseCase, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageMarketOddsUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, null, springFXMLLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ManageSettingsUseCase must not be null");

        assertThatThrownBy(() -> new LiveConsoleController(
                liveMatchTradingUseCase, calculateLiveInferenceUseCase, manageMatchUseCase, manageMarketOddsUseCase, manageSettingsUseCase, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SpringFXMLLoader must not be null");
    }

    @Test
    @DisplayName("LiveConsoleController should construct properly with valid dependencies")
    void shouldConstructWithValidDependencies() {
        LiveConsoleController controller = new LiveConsoleController(
                liveMatchTradingUseCase,
                calculateLiveInferenceUseCase,
                manageMatchUseCase,
                manageMarketOddsUseCase,
                manageSettingsUseCase,
                springFXMLLoader
        );

        assertThat(controller).isNotNull();
    }
}
