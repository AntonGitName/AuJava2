package ru.mit.spbau.antonpp.torrent.protocol.network;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class ConnectionCallback implements FutureCallback<Object> {

    private final Class callerClass;

    public ConnectionCallback(Class callerClass) {
        this.callerClass = callerClass;
    }

    @Override
    public void onSuccess(Object result) {
        log.debug("[{}] Connection handled without any exceptions", callerClass);
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("[{}] Connection handling led to an error", callerClass.toString(), t);
    }
}