package ru.mit.spbau.antonpp.torrent.client.uploader;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.ConnectionCallback;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

import static ru.mit.spbau.antonpp.torrent.client.TorrentClient.MAX_THREADS;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class UploaderPortListener implements Runnable {

    @NotNull
    private final ListeningExecutorService handleService;
    @NotNull
    private final ServerSocket serverSocket;
    @NotNull
    private final ClientFileManager fileManager;

    private boolean isRunning = true;

    public UploaderPortListener(@NotNull ServerSocket serverSocket, @NotNull ClientFileManager fileManager) {
        this.serverSocket = serverSocket;
        this.fileManager = fileManager;
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
                final ListenableFuture<?> listenableFuture =
                        handleService.submit(new UploaderConnectionHandler(clientSocket, fileManager));
                Futures.addCallback(listenableFuture, new ConnectionCallback(UploaderPortListener.class));
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
