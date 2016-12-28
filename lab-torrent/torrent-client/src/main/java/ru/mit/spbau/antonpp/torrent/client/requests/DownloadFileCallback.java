package ru.mit.spbau.antonpp.torrent.client.requests;

/**
 * @author antonpp
 * @since 14/12/2016
 */
public interface DownloadFileCallback {
    void onFinish(int id);

    void onFail(int id, Throwable e);

    void progress(int id, long downloadedSize, long fullSize);

    void noSeeds(int id);

}
