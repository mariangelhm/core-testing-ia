package core.errors;

import core.log.LoggerUtil;

/**
 * Base exception for the QA automation framework.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
        LoggerUtil.getLogger(getClass()).error(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
        LoggerUtil.getLogger(getClass()).error(message, cause);
    }
}
