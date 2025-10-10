package core.errors;

/**
 * Exception thrown when Jenkins operations fail.
 */
public class JenkinsException extends FrameworkException {

    /**
     * Creates the exception with the provided message.
     *
     * @param message error description
     */
    public JenkinsException(String message) {
        super(message);
    }

    /**
     * Creates the exception with the provided message and cause.
     *
     * @param message error description
     * @param cause original error
     */
    public JenkinsException(String message, Throwable cause) {
        super(message, cause);
    }
}
