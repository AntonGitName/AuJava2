package ru.mit.spbau.antonpp.torrent.commons.network;

import org.jetbrains.annotations.NonNls;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class ConnectionIOException extends RuntimeException {
    public ConnectionIOException(@NonNls String message) {
        super(message);
    }

    public ConnectionIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
