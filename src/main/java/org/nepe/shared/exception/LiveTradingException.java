package org.nepe.shared.exception;

/**
 * Thrown when an invalid or illegal operation is performed during live match trading
 * (e.g., modifying finished matches, invalid event transitions, chronological inconsistencies).
 */
public class LiveTradingException extends NepeException {

    public LiveTradingException(String message) {
        super(message);
    }

    public LiveTradingException(String message, Throwable cause) {
        super(message, cause);
    }
}
