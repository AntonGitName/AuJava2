package ru.mit.spbau.antonpp.torrent.client.uploader;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.TorrentClient;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.protocol.network.AbstractPortListener;
import ru.mit.spbau.antonpp.torrent.protocol.network.ConnectionCallback;

import java.net.ServerSocket;
import java.net.Socket;

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
    protected void handleNewConnnection(Socket clientSocket, ListeningExecutorService executor) {
        val listenableFuture = executor.submit(new UploaderConnectionHandler(clientSocket, fileManager));
        Futures.addCallback(listenableFuture, new ConnectionCallback(UploaderPortListener.class));
    }
}
