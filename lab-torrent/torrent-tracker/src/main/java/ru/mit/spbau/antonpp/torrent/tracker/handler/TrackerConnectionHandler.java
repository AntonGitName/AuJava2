package ru.mit.spbau.antonpp.torrent.tracker.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.protocol.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.protocol.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.protocol.network.AbstractConnectionHandler;
import ru.mit.spbau.antonpp.torrent.protocol.protocol.TrackerRequestCode;
import ru.mit.spbau.antonpp.torrent.tracker.ClientRecord;
import ru.mit.spbau.antonpp.torrent.tracker.exceptions.TrackerConnectionException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Slf4j
public class TrackerConnectionHandler extends AbstractConnectionHandler {

    @NotNull
    private final ConcurrentHashMap<Integer, FileRecord> availableFiles;
    @NotNull
    private final ConcurrentHashMap<SeedRecord, ClientRecord> activeClients;
    @NotNull
    private final AtomicInteger freeId;

    public TrackerConnectionHandler(Socket clientSocket,
                                    @NotNull ConcurrentHashMap<Integer, FileRecord> availableFiles,
                                    @NotNull ConcurrentHashMap<SeedRecord, ClientRecord> activeClients,
                                    @NotNull AtomicInteger freeId) {
        super(clientSocket);
        this.availableFiles = availableFiles;
        this.activeClients = activeClients;
        this.freeId = freeId;
    }

    private static <T> Set<T> getSetSnapshot(Set<T> set) {
        return new HashSet<>(set);
    }

    @Override
    protected void handle(DataInputStream dis, DataOutputStream dos) {
        try {
            val requestCode = dis.readInt();
            log.debug("Received request with code {}", requestCode);
            switch (requestCode) {
                case TrackerRequestCode.RQ_LIST:
                    handleList(dis, dos);
                    break;
                case TrackerRequestCode.RQ_UPLOAD:
                    handleUpload(dis, dos);
                    break;
                case TrackerRequestCode.RQ_SOURCES:
                    handleSources(dis, dos);
                    break;
                case TrackerRequestCode.RQ_UPDATE:
                    handleUpldate(dis, dos);
                    break;
                default:
                    throw new TrackerConnectionException("Unknown command");
            }
            log.debug("Request handled");
        } catch (SocketTimeoutException e) {
            log.debug("Socket read time limit exceeded", e);
        } catch (IOException e) {
            throw new TrackerConnectionException("Failed to handle request", e);
        }
    }

    private void handleUpldate(DataInputStream dis, DataOutputStream dos) throws IOException {
        val ip = new byte[4];
        ip[0] = dis.readByte();
        ip[1] = dis.readByte();
        ip[2] = dis.readByte();
        ip[3] = dis.readByte();
        val port = dis.readShort();
        val numFiles = dis.readInt();
        val seed = SeedRecord.builder().ip(ip).port(port).build();
        val time = System.currentTimeMillis();
        val files = new HashSet<Integer>(numFiles);
        for (int i = 0; i < numFiles; i++) {
            files.add(dis.readInt());
        }
        val client = ClientRecord.builder().files(files).lastUpdateTime(time).build();

        activeClients.put(seed, client);
    }

    private void handleSources(DataInputStream dis, DataOutputStream dos) throws IOException {
        val id = dis.readInt();
        log.debug("received request: SOURCES {}", id);
        val clients = getSetSnapshot(activeClients.entrySet());
        val seeds = clients.stream().filter(x -> x.getValue().getFiles().contains(id)).collect(Collectors.toList());
        dos.writeInt(seeds.size());
        for (val seed : seeds) {
            val ip = seed.getKey().getIp();
            val port = seed.getKey().getPort();
            dos.write(ip);
            dos.writeInt(port);
        }

    }

    private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
        val name = dis.readUTF();
        val size = dis.readLong();
        log.debug("received request: UPLOAD {} {}", name, size);
        final FileRecord fileRecord = FileRecord.builder().name(name).size(size).build();
        while (true) {
            val id = freeId.getAndIncrement();
            if (availableFiles.putIfAbsent(id, fileRecord) == null) {
                dos.writeInt(id);
                break;
            }
        }
    }

    private void handleList(DataInputStream dis, DataOutputStream dos) throws IOException {
        log.debug("received request: LIST");
        val entries = getSetSnapshot(availableFiles.entrySet());
        dos.writeInt(entries.size());
        for (val entry : entries) {
            dos.writeInt(entry.getKey());
            dos.writeUTF(entry.getValue().getName());
            dos.writeLong(entry.getValue().getSize());
        }
    }
}
