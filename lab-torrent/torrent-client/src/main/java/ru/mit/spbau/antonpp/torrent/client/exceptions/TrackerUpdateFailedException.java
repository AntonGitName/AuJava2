package ru.mit.spbau.antonpp.torrent.client.exceptions;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
public class TrackerUpdateFailedException extends RuntimeException {
    public TrackerUpdateFailedException(Throwable cause) {
        super(cause);
    }
}
