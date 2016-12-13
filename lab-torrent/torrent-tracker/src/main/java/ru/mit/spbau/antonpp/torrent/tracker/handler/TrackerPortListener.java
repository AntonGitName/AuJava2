package ru.mit.spbau.antonpp.torrent.tracker.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.network.AbstractPortListener;
import ru.mit.spbau.antonpp.torrent.tracker.ClientRecord;
import ru.mit.spbau.antonpp.torrent.tracker.TorrentTracker;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Slf4j
public class TrackerPortListener extends AbstractPortListener {

    @NotNull
    private final ConcurrentHashMap<Integer, FileRecord> availableFiles;
    @NotNull
    private final ConcurrentHashMap<SeedRecord, ClientRecord> activeClients;
    @NotNull
    private final AtomicInteger freeId;

    public TrackerPortListener(@NotNull ServerSocket serverSocket,
                               @NotNull ConcurrentHashMap<Integer, FileRecord> availableFiles,
                               @NotNull ConcurrentHashMap<SeedRecord, ClientRecord> activeClients,
                               @NotNull AtomicInteger freeId) {
        super(serverSocket, TorrentTracker.MAX_THREADS);
        this.availableFiles = availableFiles;
        this.activeClients = activeClients;
        this.freeId = freeId;
    }

    @Override
    protected void handleNewConnection(Socket clientSocket, ExecutorService executor) {
        executor.submit(new TrackerConnectionHandler(clientSocket, availableFiles, activeClients, freeId));
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
        log.warn("Disconnected");
    }
}
