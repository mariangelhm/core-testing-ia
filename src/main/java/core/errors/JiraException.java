package core.errors;

/**
 * Exception thrown when Jira/Xray operations fail.
 */
public class JiraException extends FrameworkException {

    public JiraException(String message) {
        super(message);
    }

    public JiraException(String message, Throwable cause) {
        super(message, cause);
    }
}
