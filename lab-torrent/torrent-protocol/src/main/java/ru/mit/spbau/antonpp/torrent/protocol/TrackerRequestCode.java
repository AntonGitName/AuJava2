package ru.mit.spbau.antonpp.torrent.protocol;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class TrackerRequestCode {
    private TrackerRequestCode() {
    }

    public static final byte RQ_LIST = (byte) 1;
    public static final byte RQ_UPLOAD = (byte) 2;
    public static final byte RQ_SOURCES = (byte) 3;
    public static final byte RQ_UPDATE = (byte) 4;
}
