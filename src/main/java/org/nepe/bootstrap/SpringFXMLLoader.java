package org.nepe.bootstrap;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.nepe.shared.exception.DomainValidationException;
import org.nepe.shared.exception.GuiException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Spring-aware FXML Loader utility.
 * <p>
 * Bridges the JavaFX view instantiation pipeline with the Spring Boot IoC container by
 * registering Spring's {@link ApplicationContext#getBean(Class)} as the controller factory.
 * This enables full dependency injection (@Autowired or constructor injection) within all
 * JavaFX @FXML controller classes.
 */
@Component
public class SpringFXMLLoader {

    private final ApplicationContext applicationContext;

    public SpringFXMLLoader(ApplicationContext applicationContext) {
        this.applicationContext = Objects.requireNonNull(applicationContext, "ApplicationContext must not be null");
    }

    /**
     * Loads a JavaFX root node from an FXML resource file with Spring dependency injection.
     *
     * @param fxmlPath classpath-relative path to the FXML file (e.g. "/views/dashboard.fxml")
     * @param <T>      expected root node type (e.g. Parent, BorderPane, etc.)
     * @return loaded root node hierarchy
     * @throws GuiException if the FXML file is not found or fails to load
     */
    public <T extends Parent> T load(String fxmlPath) {
        FXMLLoader loader = createLoader(fxmlPath);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new GuiException(String.format("Failed to load FXML view from path '%s': %s", fxmlPath, e.getMessage()), e);
        }
    }

    /**
     * Loads an FXML view and returns both the loaded root node and its injected controller instance.
     *
     * @param fxmlPath classpath-relative path to the FXML file
     * @param <T>      root node type
     * @param <C>      controller type
     * @return a {@link ViewResult} containing both the root node and the controller
     * @throws GuiException if loading fails
     */
    public <T extends Parent, C> ViewResult<T, C> loadWithController(String fxmlPath) {
        FXMLLoader loader = createLoader(fxmlPath);
        try {
            T root = loader.load();
            C controller = loader.getController();
            return new ViewResult<>(root, controller);
        } catch (IOException e) {
            throw new GuiException(String.format("Failed to load FXML view with controller from '%s': %s", fxmlPath, e.getMessage()), e);
        }
    }

    /**
     * Creates and configures a new {@link FXMLLoader} instance with Spring's controller factory.
     *
     * @param fxmlPath classpath-relative path to the FXML file
     * @return configured FXMLLoader instance
     */
    public FXMLLoader createLoader(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            throw new DomainValidationException("FXML path cannot be null or blank.");
        }
        URL resource = getClass().getResource(fxmlPath);
        if (resource == null) {
            throw new GuiException(String.format("FXML resource file not found at classpath: '%s'", fxmlPath));
        }

        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(applicationContext::getBean);
        return loader;
    }

    /**
     * Immutable container holding a loaded JavaFX root node and its associated controller.
     */
    public record ViewResult<T extends Parent, C>(T rootNode, C controller) {
        public ViewResult {
            Objects.requireNonNull(rootNode, "rootNode cannot be null");
            Objects.requireNonNull(controller, "controller cannot be null");
        }
    }
}
