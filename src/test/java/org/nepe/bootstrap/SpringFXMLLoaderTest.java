package org.nepe.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.GuiException;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("SpringFXMLLoader Unit Tests")
class SpringFXMLLoaderTest {

    @Test
    @DisplayName("Constructor should reject null ApplicationContext")
    void shouldRejectNullApplicationContext() {
        assertThatThrownBy(() -> new SpringFXMLLoader(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ApplicationContext must not be null");
    }

    @Test
    @DisplayName("createLoader should reject null or blank fxmlPath")
    void shouldRejectNullOrBlankPath() {
        ApplicationContext context = mock(ApplicationContext.class);
        SpringFXMLLoader loader = new SpringFXMLLoader(context);

        assertThatThrownBy(() -> loader.createLoader(null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("FXML path cannot be null or blank");

        assertThatThrownBy(() -> loader.createLoader("   "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("FXML path cannot be null or blank");
    }

    @Test
    @DisplayName("createLoader should throw GuiException when resource does not exist")
    void shouldThrowGuiExceptionWhenResourceNotFound() {
        ApplicationContext context = mock(ApplicationContext.class);
        SpringFXMLLoader loader = new SpringFXMLLoader(context);

        assertThatThrownBy(() -> loader.createLoader("/non_existent_view.fxml"))
                .isInstanceOf(GuiException.class)
                .hasMessageContaining("FXML resource file not found at classpath: '/non_existent_view.fxml'");
    }

    @Test
    @DisplayName("load should throw GuiException when resource is missing")
    void shouldThrowGuiExceptionOnLoadMissingResource() {
        ApplicationContext context = mock(ApplicationContext.class);
        SpringFXMLLoader loader = new SpringFXMLLoader(context);

        assertThatThrownBy(() -> loader.load("/missing.fxml"))
                .isInstanceOf(GuiException.class);
    }

    @Test
    @DisplayName("loadWithController should throw GuiException when resource is missing")
    void shouldThrowGuiExceptionOnLoadWithControllerMissingResource() {
        ApplicationContext context = mock(ApplicationContext.class);
        SpringFXMLLoader loader = new SpringFXMLLoader(context);

        assertThatThrownBy(() -> loader.loadWithController("/missing.fxml"))
                .isInstanceOf(GuiException.class);
    }

    @Test
    @DisplayName("ViewResult record should enforce non-null invariants")
    void shouldVerifyViewResultInvariants() {
        assertThatThrownBy(() -> new SpringFXMLLoader.ViewResult<>(null, "controller"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rootNode cannot be null");

        assertThatThrownBy(() -> new SpringFXMLLoader.ViewResult<>(mock(javafx.scene.Parent.class), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("controller cannot be null");
    }
}
