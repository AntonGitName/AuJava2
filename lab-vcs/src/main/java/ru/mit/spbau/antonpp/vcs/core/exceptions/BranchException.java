package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author antonpp
 * @since 28/10/16
 */
public class BranchException extends Exception {
    public BranchException(String message) {
        super(message);
    }

    public BranchException(String message, Throwable cause) {

        super(message, cause);
    }
}
