package ru.mit.spbau.antonpp.torrent.tracker;

import lombok.val;
import ru.mit.spbau.antonpp.torrent.protocol.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.protocol.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.protocol.network.ConnectionException;
import ru.mit.spbau.antonpp.torrent.protocol.serialization.FileSerializable;
import ru.mit.spbau.antonpp.torrent.protocol.serialization.SerializationException;
import ru.mit.spbau.antonpp.torrent.tracker.exceptions.TrackerStartException;
import ru.mit.spbau.antonpp.torrent.tracker.handler.TrackerPortListener;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author antonpp
 * @since 12/12/2016
 */
public class TorrentTracker implements FileSerializable, Closeable {

    public static final int MAX_THREADS = 8;
    private static final long FIVE_MINUTES = 1000 * 60 * 5;

    private ConcurrentHashMap<Integer, FileRecord> availableFiles = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SeedRecord, ClientRecord> activeClients = new ConcurrentHashMap<>();
    private AtomicInteger freeId = new AtomicInteger();

    private ScheduledExecutorService updateClientsExecutor;
    private ExecutorService listenService;
    private TrackerPortListener portListener;
    private Path path;

    public static TorrentTracker create(Path path, short port) throws TrackerStartException {
        val tracker = new TorrentTracker();
        if (Files.exists(path)) {
            tracker.deserialize(path);
        }
        try {
            tracker.start(port);
        } catch (ConnectionException e) {
            throw new TrackerStartException(e);
        }
        return tracker;
    }

    private void start(short port) {
        updateClientsExecutor = Executors.newSingleThreadScheduledExecutor();
        updateClientsExecutor.schedule(() -> {
            val clients = activeClients.values();
            val currentTime = System.currentTimeMillis();
            val oldClients = clients.stream().filter(x -> currentTime - x.getLastUpdateTime() > FIVE_MINUTES)
                    .collect(Collectors.toList());
            clients.removeAll(oldClients);
        }, 1, TimeUnit.MINUTES);

        final ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new ConnectionException("Could not start server", e);
        }
        portListener = new TrackerPortListener(serverSocket, availableFiles, activeClients, freeId);
        listenService = Executors.newSingleThreadExecutor();
        listenService.execute(portListener);
    }

    @Override
    public void serialize(Path path) {
        this.path = path;
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            os.writeObject(availableFiles);
            os.writeObject(activeClients);
            os.writeObject(freeId);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize ClientFileManager", e);
        }
    }

    @Override
    public void deserialize(Path path) {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            activeClients = (ConcurrentHashMap<SeedRecord, ClientRecord>) os.readObject();
            availableFiles = (ConcurrentHashMap<Integer, FileRecord>) os.readObject();
            freeId = (AtomicInteger) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    @Override
    public void close() throws IOException {
        portListener.stop();
        listenService.shutdown();
        updateClientsExecutor.shutdown();
        deserialize(path);
    }
}
