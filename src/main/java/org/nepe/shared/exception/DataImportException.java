package org.nepe.shared.exception;

/**
 * Thrown when an error occurs during CSV parsing or data ingestion (e.g., missing headers, malformed date/time, corrupt rows).
 */
public class DataImportException extends NepeException {

    private final Integer lineNumber;

    public DataImportException(String message) {
        super(message);
        this.lineNumber = null;
    }

    public DataImportException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = null;
    }

    public DataImportException(String message, int lineNumber) {
        super(String.format("Error at line %d: %s", lineNumber, message));
        this.lineNumber = lineNumber;
    }

    public DataImportException(String message, int lineNumber, Throwable cause) {
        super(String.format("Error at line %d: %s", lineNumber, message), cause);
        this.lineNumber = lineNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}
