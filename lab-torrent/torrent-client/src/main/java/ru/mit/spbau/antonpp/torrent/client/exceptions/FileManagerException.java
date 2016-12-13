package ru.mit.spbau.antonpp.torrent.client.exceptions;

import org.jetbrains.annotations.NonNls;

/**
 * @author antonpp
 * @since 13/12/2016
 */
public class FileManagerException extends RuntimeException {
    public FileManagerException(@NonNls String message) {
        super(message);
    }
}
