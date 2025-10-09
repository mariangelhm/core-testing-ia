package core.errors;

/**
 * Exception thrown when database operations fail.
 */
public class DBException extends FrameworkException {

    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}
