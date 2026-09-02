package org.nepe.bootstrap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.nepe.NepeApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URL;

/**
 * JavaFX Application lifecycle coordinator integrated with Spring Boot.
 * <p>
 * Manages the JavaFX application lifecycle:
 * <ul>
 *     <li>{@link #init()}: Bootstraps the Spring {@link ConfigurableApplicationContext}.</li>
 *     <li>{@link #start(Stage)}: Configures and displays the primary application window using {@link SpringFXMLLoader}.</li>
 *     <li>{@link #stop()}: Gracefully closes the Spring context and releases desktop system resources.</li>
 * </ul>
 */
public class JavaFxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JavaFxApplication.class);

    private static final String MAIN_VIEW_PATH = "/views/dashboard.fxml";
    private static final String STYLESHEET_PATH = "/styles.css";
    private static final String APPLICATION_TITLE = "NEPE - Nexus Exchange Prediction Engine";
    private static final double MIN_WIDTH = 1100.0;
    private static final double MIN_HEIGHT = 700.0;
    private static final double AUTO_MAXIMIZE_THRESHOLD_WIDTH = 1440.0;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        log.info("Initializing NEPE Spring Boot context...");
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = new SpringApplicationBuilder()
                .sources(NepeApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting JavaFX Primary Stage...");

        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) ->
                log.error("Uncaught exception on JavaFX Application Thread in thread [{}]: {}",
                        thread.getName(), throwable.getMessage(), throwable)
        );

        SpringFXMLLoader fxmlLoader = applicationContext.getBean(SpringFXMLLoader.class);
        Parent rootNode = fxmlLoader.load(MAIN_VIEW_PATH);

        Scene scene = new Scene(rootNode, MIN_WIDTH, MIN_HEIGHT);

        URL cssResource = getClass().getResource(STYLESHEET_PATH);
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setTitle(APPLICATION_TITLE);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setScene(scene);

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        if (visualBounds != null && visualBounds.getWidth() <= AUTO_MAXIMIZE_THRESHOLD_WIDTH) {
            log.info("Compact display detected (visualBounds width: {}px <= {}px). Auto-maximizing primary stage.",
                    visualBounds.getWidth(), AUTO_MAXIMIZE_THRESHOLD_WIDTH);
            primaryStage.setMaximized(true);
        }

        primaryStage.setOnCloseRequest(event -> {
            log.info("Primary stage close requested by user. Terminating JavaFX application.");
            Platform.exit();
        });
        primaryStage.show();
        log.info("NEPE JavaFX UI displayed successfully.");
    }

    @Override
    public void stop() {
        log.info("Shutting down NEPE application and closing Spring context...");
        if (applicationContext != null && applicationContext.isActive()) {
            try {
                applicationContext.close();
                log.info("Spring ApplicationContext closed successfully.");
            } catch (Exception e) {
                log.error("Error during Spring ApplicationContext shutdown: {}", e.getMessage(), e);
            }
        }
        Platform.exit();
    }
}
