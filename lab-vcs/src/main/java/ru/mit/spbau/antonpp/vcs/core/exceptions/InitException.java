package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author antonpp
 * @since 28/10/16
 */
public class InitException extends Exception {
    public InitException(String message) {
        super(message);
    }

    public InitException(Throwable cause) {
        super("Failed to create internal files", cause);
    }
}
