package ru.mit.spbau.antonpp.torrent.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.client.requester.ClientRequester;
import ru.mit.spbau.antonpp.torrent.client.updater.ClientStatusUpdater;
import ru.mit.spbau.antonpp.torrent.client.uploader.UploaderPortListener;
import ru.mit.spbau.antonpp.torrent.protocol.data.FileRecord;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public final class TorrentClient implements Closeable {

    public static final int MAX_THREADS = 4;

    private final ScheduledExecutorService updateExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService listenService = Executors.newSingleThreadExecutor();
    private final UploaderPortListener portListener;
    private final ClientRequester requester;
    private final ClientFileManager fileManager;


    public TorrentClient(String host, short trackerPort, short clientPort, @NotNull ClientFileManager fileManager)
            throws TorrentClientException {
        this.fileManager = fileManager;
        final ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(clientPort);
        } catch (IOException e) {
            throw new TorrentClientException("Could not start torrent client", e);
        }
        portListener = new UploaderPortListener(serverSocket, fileManager);
        listenService.execute(portListener);
        val updater = new ClientStatusUpdater(host, trackerPort, fileManager, clientPort);
        updateExecutor.schedule(updater, 5, TimeUnit.MINUTES);
        requester = new ClientRequester(host, trackerPort);
    }

    @Override
    public void close() throws IOException {
        portListener.stop();
        listenService.shutdown();
        updateExecutor.shutdown();
    }

    public void requestDownloadFileAsync(int id, ClientRequester.DownloadFileCallback callback) {
        requester.requestDownloadFileAsync(id, fileManager, callback);
    }

    public Map<Integer, FileRecord> requestFilesList() throws RequestFailedException {
        return requester.requestFilesList();
    }

    public int requestUploadFile(String pathToFile) throws RequestFailedException {
        val path = Paths.get(pathToFile);
        try {
            val id = requester.requestUploadFile(path.getFileName().toString(), Files.size(path));
            fileManager.saveFile(path, id);
            return id;
        } catch (IOException e) {
            throw new RequestFailedException(e);
        }

    }
}
