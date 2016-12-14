package ru.mit.spbau.antonpp.torrent.client;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientStartException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.client.files.FileHolder;
import ru.mit.spbau.antonpp.torrent.client.requests.ClientRequester;
import ru.mit.spbau.antonpp.torrent.client.requests.ClientStatusUpdater;
import ru.mit.spbau.antonpp.torrent.client.requests.DownloadFileCallback;
import ru.mit.spbau.antonpp.torrent.client.uploader.UploaderPortListener;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public final class TorrentClient implements Closeable {

    final ClientFileManager fileManager;
    private final ListeningScheduledExecutorService updateExecutor =
            MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
    private final ExecutorService listenService = Executors.newSingleThreadExecutor();
    private final UploaderPortListener portListener;
    private final ClientRequester requester;
    private final ClientStatusUpdater updater;

    public TorrentClient(String host, int trackerPort, int clientPort, FutureCallback<Object> updateCallback, Path dir)
            throws TorrentClientStartException {

        if (!checkPort(trackerPort) || !checkPort(clientPort)) {
            throw new TorrentClientStartException("Bad port value");
        }

        final ServerSocket serverSocket;
        try {
            fileManager = new ClientFileManager(dir);
            serverSocket = new ServerSocket(clientPort);
        } catch (IOException | RuntimeException e) {
            throw new TorrentClientStartException(e);
        }
        portListener = new UploaderPortListener(serverSocket, fileManager);
        listenService.execute(portListener);
        updater = new ClientStatusUpdater(host, trackerPort, fileManager, clientPort);
        val future = updateExecutor.scheduleAtFixedRate(updater, 0, 5, TimeUnit.MINUTES);
        Futures.addCallback(future, updateCallback);
        requester = new ClientRequester(host, trackerPort);
    }

    private static boolean checkPort(int port) {
        return (port >= 0 && port <= 65535);
    }

    @Override
    public void close() throws IOException {
        requester.close();
        updateExecutor.shutdownNow();
        portListener.stop();
        listenService.shutdown();
    }

    public void requestDownloadFile(int id, String destination, DownloadFileCallback callback)
            throws RequestFailedException {
        requester.requestDownloadFile(id, fileManager, new SaveFileCallback(callback, Paths.get(destination)));
    }

    public Map<Integer, TrackerFileRecord> requestFilesList() throws RequestFailedException {
        return requester.requestFilesList();
    }

    public TrackerFileRecord requestUploadFile(String pathToFile, String name) throws RequestFailedException {
        val path = Paths.get(pathToFile);
        try {
            val record = requester.requestUploadFile(name, Files.size(path));
            fileManager.saveFile(path, record);
            updateExecutor.submit(updater).get();
            return record;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RequestFailedException(e);
        }

    }

    public Set<Integer> requestFilePartsList(int id) {
        return fileManager.getAvailableParts(id);
    }

    public List<FileHolder.ClientFileRecord> requestLocalFiles() {
        return fileManager.getLocalRecords();
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
