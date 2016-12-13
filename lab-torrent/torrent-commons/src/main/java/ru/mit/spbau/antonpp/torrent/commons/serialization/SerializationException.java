package ru.mit.spbau.antonpp.torrent.commons.serialization;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class SerializationException extends RuntimeException {
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
