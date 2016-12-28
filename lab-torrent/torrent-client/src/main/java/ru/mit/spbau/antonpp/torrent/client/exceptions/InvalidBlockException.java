package ru.mit.spbau.antonpp.torrent.client.exceptions;

import java.io.IOException;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class InvalidBlockException extends IOException {
    public InvalidBlockException(String message) {
        super(message);
    }
}
