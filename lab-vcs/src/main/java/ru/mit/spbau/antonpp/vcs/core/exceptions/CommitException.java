package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class CommitException extends Exception {
    public CommitException(String message) {
        super(message);
    }

    public CommitException(String message, Throwable cause) {
        super(message, cause);
    }
}
