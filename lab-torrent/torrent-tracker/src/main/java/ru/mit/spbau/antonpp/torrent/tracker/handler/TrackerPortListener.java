package ru.mit.spbau.antonpp.torrent.tracker.handler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.protocol.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.protocol.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.protocol.network.AbstractPortListener;
import ru.mit.spbau.antonpp.torrent.protocol.network.ConnectionCallback;
import ru.mit.spbau.antonpp.torrent.tracker.ClientRecord;
import ru.mit.spbau.antonpp.torrent.tracker.TorrentTracker;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author antonpp
 * @since 12/12/2016
 */
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
    protected void handleNewConnnection(Socket clientSocket, ListeningExecutorService executor) {
        val listenableFuture = executor.submit(new TrackerConnectionHandler(clientSocket, availableFiles, activeClients, freeId));
        Futures.addCallback(listenableFuture, new ConnectionCallback(TrackerPortListener.class));
    }
}
