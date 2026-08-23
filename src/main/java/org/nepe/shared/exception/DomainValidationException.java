package org.nepe.shared.exception;

/**
 * Thrown when a business rule, invariant, or mathematical constraint is violated within the domain.
 */
public class DomainValidationException extends NepeException {

    public DomainValidationException(String message) {
        super(message);
    }

    public DomainValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
