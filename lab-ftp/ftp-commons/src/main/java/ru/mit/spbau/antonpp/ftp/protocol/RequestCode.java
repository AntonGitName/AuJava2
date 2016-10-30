package ru.mit.spbau.antonpp.ftp.protocol;

/**
 * @author antonpp
 * @since 29/10/2016
 */
public class RequestCode {
    public static final int RQ_LIST = 1;
    public static final int RQ_GET = 2;
    public static final int RQ_DISCONNECT = 4;

    private RequestCode() {
    }
}
