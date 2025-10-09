package core.errors;

/**
 * Exception thrown when Jenkins operations fail.
 */
public class JenkinsException extends FrameworkException {

    public JenkinsException(String message) {
        super(message);
    }

    public JenkinsException(String message, Throwable cause) {
        super(message, cause);
    }
}
