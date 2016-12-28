package ru.mit.spbau.antonpp.ftp.server.exceptions;

/**
 * @author antonpp
 * @since 30/10/2016
 */
public class FtpServerException extends Exception {
    public FtpServerException(String message) {
        super(message);
    }

    public FtpServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
