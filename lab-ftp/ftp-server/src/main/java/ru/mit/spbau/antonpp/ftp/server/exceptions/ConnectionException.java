package ru.mit.spbau.antonpp.ftp.server.exceptions;

import org.jetbrains.annotations.NonNls;

/**
 * @author antonpp
 * @since 29/10/2016
 */
public class ConnectionException extends RuntimeException {
    public ConnectionException(@NonNls String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
