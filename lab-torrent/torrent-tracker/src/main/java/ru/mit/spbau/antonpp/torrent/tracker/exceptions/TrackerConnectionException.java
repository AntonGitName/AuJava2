package ru.mit.spbau.antonpp.torrent.tracker.exceptions;

import org.jetbrains.annotations.NonNls;

/**
 * @author antonpp
 * @since 12/12/2016
 */
public class TrackerConnectionException extends RuntimeException {
    public TrackerConnectionException(@NonNls String message) {
        super(message);
    }

    public TrackerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
