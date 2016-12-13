package ru.mit.spbau.antonpp.torrent.client.exceptions;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
public class TrackerUnavailableException extends RuntimeException {
    public TrackerUnavailableException(Throwable cause) {
        super(cause);
    }
}
