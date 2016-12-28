package ru.mit.spbau.antonpp.torrent.commons.protocol;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class TrackerRequestCode {
    public static final byte RQ_LIST = (byte) 1;
    public static final byte RQ_UPLOAD = (byte) 2;
    public static final byte RQ_SOURCES = (byte) 3;
    public static final byte RQ_UPDATE = (byte) 4;


    private TrackerRequestCode() {
    }
}
