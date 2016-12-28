package ru.mit.spbau.antonpp.torrent.client.exceptions;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;

/**
 * @author antonpp
 * @since 13/12/2016
 */
public class FileManagerException extends IOException {
    public FileManagerException(@NonNls String message) {
        super(message);
    }
}
