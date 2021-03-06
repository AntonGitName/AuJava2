package ru.mit.spbau.antonpp.torrent.client.exceptions;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class TorrentClientStartException extends Exception {
    public TorrentClientStartException(String message) {
        super(message);
    }

    public TorrentClientStartException(Throwable cause) {
        super(cause);
    }
}
