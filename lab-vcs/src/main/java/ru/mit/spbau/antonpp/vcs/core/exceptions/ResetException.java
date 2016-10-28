package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class ResetException extends Exception {
    public ResetException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResetException(String message) {

        super(message);
    }

    public ResetException(Throwable cause) {
        super(cause);
    }
}
