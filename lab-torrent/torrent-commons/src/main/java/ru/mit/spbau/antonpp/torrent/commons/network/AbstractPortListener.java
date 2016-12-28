package ru.mit.spbau.antonpp.torrent.commons.network;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author antonpp
 * @since 12/12/2016
 */
public abstract class AbstractPortListener implements Runnable {
    @NotNull
    private final ExecutorService handleService;
    @NotNull
    private final ServerSocket serverSocket;

    private boolean isRunning = true;

    public AbstractPortListener(@NotNull ServerSocket serverSocket, int maxThreads) {
        this.serverSocket = serverSocket;
        handleService = Executors.newFixedThreadPool(maxThreads);
    }

    protected abstract void handleNewConnection(Socket clientSocket, ExecutorService executor);

    protected abstract void onConnect();

    protected abstract void onDisconnect();

    protected abstract void onConnectionFail();

    @Override
    public void run() {
        while (isRunning) {
            try {
                final Socket clientSocket;
                clientSocket = serverSocket.accept();
                onConnect();
                handleNewConnection(clientSocket, handleService);
                onDisconnect();
            } catch (IOException e) {
                onConnectionFail();
            }
        }
    }

    public void stop() throws IOException {
        isRunning = false;
        handleService.shutdownNow();
        serverSocket.close();
    }
}
