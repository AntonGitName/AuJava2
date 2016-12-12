package ru.mit.spbau.antonpp.torrent.protocol.protocol;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public final class ClientRequestCode {

    public static final byte RQ_STAT = (byte) 1;
    public static final byte RQ_GET = (byte) 2;

    private ClientRequestCode() {
    }
}
