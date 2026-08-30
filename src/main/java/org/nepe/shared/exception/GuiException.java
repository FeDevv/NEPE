package org.nepe.shared.exception;

/**
 * Unchecked exception thrown when a JavaFX GUI or view rendering/loading error occurs.
 * <p>
 * Signals failures such as missing FXML templates, view initialization errors,
 * controller factory misconfigurations, or modal navigation issues.
 */
public class GuiException extends NepeException {

    public GuiException(String message) {
        super(message);
    }

    public GuiException(String message, Throwable cause) {
        super(message, cause);
    }
}
