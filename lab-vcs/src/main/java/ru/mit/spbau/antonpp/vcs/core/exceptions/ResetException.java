package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class ResetException extends Exception {
    public ResetException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResetException() {
        super("File is not in stage and not versioned in HEAD. Cannot reset.");
    }
}
