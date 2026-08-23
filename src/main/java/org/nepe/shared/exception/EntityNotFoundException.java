package org.nepe.shared.exception;

/**
 * Thrown when a requested domain entity (e.g., Competition, Team, Match, Alias) cannot be found.
 */
public class EntityNotFoundException extends NepeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityType, Object identifier) {
        super(String.format("%s with identifier '%s' was not found.", entityType, identifier));
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
