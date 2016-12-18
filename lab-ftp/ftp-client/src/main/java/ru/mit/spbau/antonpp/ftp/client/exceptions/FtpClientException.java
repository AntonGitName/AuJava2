package ru.mit.spbau.antonpp.ftp.client.exceptions;

/**
 * @author antonpp
 * @since 30/10/2016
 */
public class FtpClientException extends Exception {
    public FtpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
