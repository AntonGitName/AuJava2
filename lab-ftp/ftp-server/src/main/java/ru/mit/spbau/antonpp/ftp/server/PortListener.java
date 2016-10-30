package ru.mit.spbau.antonpp.ftp.server;

import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import static ru.mit.spbau.antonpp.ftp.server.FtpServer.MAX_THREADS;

/**
 * @author antonpp
 * @since 30/10/2016
 */
@Slf4j
public class PortListener implements Runnable {

    @NotNull
    private final ListeningExecutorService handleService;
    @NotNull
    private final ServerSocket serverSocket;

    private boolean isRunning = true;

    public PortListener(@NotNull ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        handleService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(MAX_THREADS));
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                log.debug("Waiting for a new connection...");
                final Socket clientSocket;
                clientSocket = serverSocket.accept();
                log.debug("Accepted a connection");
                final ListenableFuture<?> listenableFuture = handleService.submit(new ConnectionHandler(clientSocket));
                Futures.addCallback(listenableFuture, new ConnectionCallback());
            } catch (IOException e) {
                log.error("Could not accept connection", e);
            }
        }
    }

    public void stop() throws IOException {
        isRunning = false;
        serverSocket.close();
        handleService.shutdownNow();
    }

    private static final class ConnectionCallback implements FutureCallback<Object> {

        @Override
        public void onSuccess(Object result) {
            log.debug("Connection handled without any exceptions");
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Connection handling led to an error", t);
        }
    }
}