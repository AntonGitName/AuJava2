package ru.mit.spbau.antonpp.torrent.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientStartException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.client.requests.ClientRequester;
import ru.mit.spbau.antonpp.torrent.client.requests.ClientRequester.DownloadFileCallback;
import ru.mit.spbau.antonpp.torrent.client.requests.ClientStatusUpdater;
import ru.mit.spbau.antonpp.torrent.client.uploader.UploaderPortListener;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public final class TorrentClient implements Closeable {

    public static final int MAX_THREADS = 4;
    final ClientFileManager fileManager;
    private final ListeningScheduledExecutorService updateExecutor =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
    private final ExecutorService listenService = Executors.newSingleThreadExecutor();
    private final UploaderPortListener portListener;
    private final ClientRequester requester;

    public TorrentClient(String host, short trackerPort, short clientPort, FutureCallback<Object> updateCallback, Path dir)
            throws TorrentClientStartException {

        final ServerSocket serverSocket;
        try {
            fileManager = ClientFileManager.load(dir);
            serverSocket = new ServerSocket(clientPort);
        } catch (IOException e) {
            throw new TorrentClientStartException(e);
        }
        portListener = new UploaderPortListener(serverSocket, fileManager);
        listenService.execute(portListener);
        val updater = new ClientStatusUpdater(host, trackerPort, fileManager, clientPort);
        val future = updateExecutor.scheduleAtFixedRate(updater, 1, 5 * 60, TimeUnit.SECONDS);
        Futures.addCallback(future, updateCallback);
        requester = new ClientRequester(host, trackerPort);
    }

    @Override
    public void close() throws IOException {
        requester.close();
        updateExecutor.shutdownNow();
        portListener.stop();
        listenService.shutdown();
        fileManager.serialize();
    }

    public void requestDownloadFileAsync(int id, String destination, DownloadFileCallback callback) {
        requester.requestDownloadFile(id, fileManager, new SaveFileCallback(callback, Paths.get(destination)));
    }

    public Map<Integer, FileRecord> requestFilesList() throws RequestFailedException {
        return requester.requestFilesList();
    }

    public int requestUploadFile(String pathToFile) throws RequestFailedException {
        val path = Paths.get(pathToFile);
        try {
            val id = requester.requestUploadFile(path.getFileName().toString(), Files.size(path));
            fileManager.saveFile(path, id);
            fileManager.serialize();
            return id;
        } catch (IOException e) {
            throw new RequestFailedException(e);
        }

    }

    public List<LocalFileRecord> requestLocalFiles() throws RequestFailedException {
        val trackerList = requestFilesList();
        val localFiles = fileManager.getAvailableFiles();
        final List<LocalFileRecord> result = new ArrayList<>(localFiles.size());
        try {
            for (val id : localFiles) {

                result.add(LocalFileRecord.builder()
                        .downloadedSize(fileManager.getSize(id))
                        .name(trackerList.get(id).getName())
                        .id(id)
                        .realSize(trackerList.get(id).getSize())
                        .build());
            }
        } catch (IOException e) {
            throw new RequestFailedException(e);
        }
        return result;
    }

    @Data
    @Builder
    public static final class LocalFileRecord {
        private final String name;
        private final int id;
        private final long downloadedSize;
        private final long realSize;
    }

    private final class SaveFileCallback implements DownloadFileCallback {

        private final DownloadFileCallback appCallback;
        private final Path destination;

        private SaveFileCallback(DownloadFileCallback appCallback, Path destination) {
            this.appCallback = appCallback;
            this.destination = destination;
        }

        @Override
        public void onFinish(int id) {
            try {
                fileManager.getFile(destination, id);
                fileManager.serialize();
            } catch (IOException e) {
                appCallback.onFail(id, e);
            }
            appCallback.onFinish(id);
        }

        @Override
        public void onFail(int id, Throwable e) {
            appCallback.onFail(id, e);
        }

        @Override
        public void progress(int id, long downloadedSize, long fullSize) {
            appCallback.progress(id, downloadedSize, fullSize);
        }

        @Override
        public void noSeeds(int id) {
            appCallback.noSeeds(id);
        }
    }
}
