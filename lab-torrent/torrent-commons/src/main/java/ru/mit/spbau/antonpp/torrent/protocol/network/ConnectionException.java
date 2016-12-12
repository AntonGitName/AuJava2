package ru.mit.spbau.antonpp.torrent.protocol.network;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class ConnectionException extends RuntimeException {
    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
