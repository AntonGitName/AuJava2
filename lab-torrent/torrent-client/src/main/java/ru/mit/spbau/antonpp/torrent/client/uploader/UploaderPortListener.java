package ru.mit.spbau.antonpp.torrent.client.uploader;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.TorrentClient;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.network.AbstractPortListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class UploaderPortListener extends AbstractPortListener {

    @NotNull
    private final ClientFileManager fileManager;

    public UploaderPortListener(@NotNull ServerSocket serverSocket, @NotNull ClientFileManager fileManager) {
        super(serverSocket, TorrentClient.MAX_THREADS);
        this.fileManager = fileManager;
    }


    @Override
    protected void handleNewConnection(Socket clientSocket, ExecutorService executor) {
        executor.submit(new UploaderConnectionHandler(clientSocket, fileManager));
    }

    @Override
    protected void onConnect() {
        log.debug("Connected");
    }

    @Override
    protected void onDisconnect() {
        log.debug("Disconnected");
    }

    @Override
    protected void onConnectionFail() {
        log.warn("Connection failed");
    }
}
