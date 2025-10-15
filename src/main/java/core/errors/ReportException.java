package core.errors;

/**
 * Exception thrown when the reporting subsystem cannot persist execution data.
 */
public class ReportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a message and root cause.
     *
     * @param message description of the failure
     * @param cause   underlying exception
     */
    public ReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
