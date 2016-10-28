package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class CommitException extends Exception {
    public CommitException(Throwable cause) {
        super(cause);
    }

    public CommitException(String message, Throwable cause) {
        super(message, cause);
    }
}
