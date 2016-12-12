package ru.mit.spbau.antonpp.torrent.protocol.network;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Slf4j
public abstract class AbstractPortListener implements Runnable {
    @NotNull
    private final ListeningExecutorService handleService;
    @NotNull
    private final ServerSocket serverSocket;

    private boolean isRunning = true;

    public AbstractPortListener(@NotNull ServerSocket serverSocket, int maxThreads) {
        this.serverSocket = serverSocket;
        handleService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(maxThreads));
    }

    protected abstract void handleNewConnnection(Socket clientSocket, ListeningExecutorService executor);

    @Override
    public void run() {
        while (isRunning) {
            try {
                log.debug("Waiting for a new connection...");
                final Socket clientSocket;
                clientSocket = serverSocket.accept();
                log.debug("Accepted a connection");
                handleNewConnnection(clientSocket, handleService);
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
}
