package ru.mit.spbau.antonpp.torrent.client.exceptions;

/**
 * @author antonpp
 * @since 14/12/2016
 */
public class NoSuchFileException extends Exception {
    public NoSuchFileException(int id) {
        super("File with id=" + id + " was not found on tracker.");
    }
}
