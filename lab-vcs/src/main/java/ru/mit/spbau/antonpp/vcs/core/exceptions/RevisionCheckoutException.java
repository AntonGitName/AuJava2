package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class RevisionCheckoutException extends Exception {
    public RevisionCheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}