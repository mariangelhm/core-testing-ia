package core.errors;

/**
 * Exception thrown when Jira/Xray operations fail.
 */
public class JiraException extends FrameworkException {

    /**
     * Creates the exception with the provided message.
     *
     * @param message error description
     */
    public JiraException(String message) {
        super(message);
    }

    /**
     * Creates the exception with the provided message and cause.
     *
     * @param message error description
     * @param cause original error
     */
    public JiraException(String message, Throwable cause) {
        super(message, cause);
    }
}
