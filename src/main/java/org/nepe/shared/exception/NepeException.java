package org.nepe.shared.exception;

/**
 * Base abstract unchecked exception for all application and domain exceptions in NEPE.
 * <p>
 * By extending {@link RuntimeException}, it prevents boilerplate throws signatures across the domain core
 * while enabling centralized exception handling, translation, and structured error reporting.
 */
public abstract class NepeException extends RuntimeException {

    protected NepeException(String message) {
        super(message);
    }

    protected NepeException(String message, Throwable cause) {
        super(message, cause);
    }

    protected NepeException(Throwable cause) {
        super(cause);
    }

}
