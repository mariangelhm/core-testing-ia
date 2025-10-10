package core.errors;

/**
 * Exception thrown when database operations fail.
 */
public class DBException extends FrameworkException {

    /**
     * Creates the exception with the provided message.
     *
     * @param message error description
     */
    public DBException(String message) {
        super(message);
    }

    /**
     * Creates the exception with the provided message and cause.
     *
     * @param message error description
     * @param cause original error
     */
    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}
