package ru.mit.spbau.antonpp.torrent.client.exceptions;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class SerializationException extends RuntimeException {
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
