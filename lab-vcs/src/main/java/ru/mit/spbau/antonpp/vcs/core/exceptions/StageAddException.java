package ru.mit.spbau.antonpp.vcs.core.exceptions;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class StageAddException extends Exception {
    public StageAddException(Throwable cause) {
        super("Could not add changes to stage", cause);
    }
}
