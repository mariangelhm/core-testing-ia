package core.errors;

import core.log.LoggerUtil;

/**
 * Base exception for the QA automation framework.
 */
public class FrameworkException extends RuntimeException {

    /**
     * Creates the exception logging the provided message.
     *
     * @param message error description
     */
    public FrameworkException(String message) {
        super(message);
        LoggerUtil.getLogger(getClass()).error(message);
    }

    /**
     * Creates the exception logging the provided message and cause.
     *
     * @param message error description
     * @param cause original error
     */
    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
        LoggerUtil.getLogger(getClass()).error(message, cause);
    }
}
