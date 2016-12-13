package ru.mit.spbau.antonpp.torrent.tracker;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.serialization.FileSerializable;
import ru.mit.spbau.antonpp.torrent.commons.serialization.SerializationException;
import ru.mit.spbau.antonpp.torrent.tracker.exceptions.TrackerStartException;
import ru.mit.spbau.antonpp.torrent.tracker.handler.TrackerPortListener;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Slf4j
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

    private TorrentTracker() {
    }

    public static TorrentTracker create(Path path, short port) throws TrackerStartException {
        val tracker = new TorrentTracker();
        tracker.path = path;
        try {
            if (Files.exists(path)) {
                tracker.deserialize();
            } else {
                Files.createDirectories(path.getParent());
            }
            tracker.start(port);
        } catch (IOException e) {
            throw new TrackerStartException(e);
        }
        return tracker;
    }

    private void start(short port) throws IOException {
        val serverSocket = new ServerSocket(port);
        portListener = new TrackerPortListener(serverSocket, availableFiles, activeClients, freeId);

        listenService = Executors.newSingleThreadExecutor();
        listenService.execute(portListener);

        updateClientsExecutor = Executors.newSingleThreadScheduledExecutor();
        updateClientsExecutor.scheduleAtFixedRate(new UpdateActiveClientsRunnable(), 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void serialize() {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            os.writeObject(availableFiles);
            os.writeObject(activeClients);
            os.writeObject(freeId);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize TorrentTracker", e);
        }
    }

    @Override
    public void deserialize() {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            availableFiles = (ConcurrentHashMap<Integer, FileRecord>) os.readObject();
            activeClients = (ConcurrentHashMap<SeedRecord, ClientRecord>) os.readObject();
            freeId = (AtomicInteger) os.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new SerializationException("Could not deserialize TorrentTracker", e);
        }
    }

    @Override
    public void close() throws IOException {
        updateClientsExecutor.shutdownNow();
        portListener.stop();
        listenService.shutdown();
        serialize();
    }

    public List<FileRecord> getFiles() {
        return new ArrayList<>(availableFiles.values());
    }

    public List<SeedRecord> getUsers() {
        return new ArrayList<>(activeClients.keySet());
    }

    private final class UpdateActiveClientsRunnable implements Runnable {

        @Override
        public void run() {
            val currentTime = System.currentTimeMillis();
            val clients = activeClients.values();
            val oldClients = clients.stream()
                    .filter(x -> currentTime - x.getLastUpdateTime() > FIVE_MINUTES)
                    .collect(Collectors.toList());
            clients.removeAll(oldClients);
            log.info(generateLogMessage());
        }

        private String generateLogMessage() {
            val lines = activeClients.entrySet().stream().map(x -> {
                final byte[] ip = x.getKey().getIp();
                final String ipStr = Util.ipToStr(ip);
                return String.format("ip=%s, port=%s", ipStr, x.getKey().getPort());
            }).collect(Collectors.toList());
            return String.format("Active clients: %d\n", lines.size()) +
                    IntStream.range(0, lines.size())
                            .mapToObj(x -> String.format("%d. %s\n", x, lines.get(x)))
                            .collect(Collectors.joining());
        }
    }
}
