package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class LogWriteException extends Exception {
    public LogWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
