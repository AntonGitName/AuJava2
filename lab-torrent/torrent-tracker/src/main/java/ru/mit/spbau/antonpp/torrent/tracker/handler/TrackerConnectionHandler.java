package ru.mit.spbau.antonpp.torrent.tracker.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;
import ru.mit.spbau.antonpp.torrent.commons.network.AbstractConnectionHandler;
import ru.mit.spbau.antonpp.torrent.commons.network.ConnectionIOException;
import ru.mit.spbau.antonpp.torrent.commons.protocol.CommonRequestCode;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;
import ru.mit.spbau.antonpp.torrent.tracker.ClientRecord;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private final ConcurrentHashMap<Integer, TrackerFileRecord> availableFiles;
    @NotNull
    private final ConcurrentHashMap<SeedRecord, ClientRecord> activeClients;
    @NotNull
    private final AtomicInteger freeId;

    public TrackerConnectionHandler(Socket clientSocket,
                                    @NotNull ConcurrentHashMap<Integer, TrackerFileRecord> availableFiles,
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
    protected void onConnected() {
        log.debug("Connected");
    }

    @Override
    protected void onDisconnected() {
        log.debug("Disconnected");
    }

    @Override
    protected void handle(byte requestCode, DataInputStream dis, DataOutputStream dos) {
        try {
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
                    handleUpdate(dis, dos);
                    break;
                case CommonRequestCode.RQ_DC:
                    disconnect();
                    break;
                default:
                    throw new ConnectionIOException("Unknown command");
            }
            log.debug("Request handled");
        } catch (IOException e) {
            throw new ConnectionIOException("Failed to handle request", e);
        }
    }

    private void handleUpdate(DataInputStream dis, DataOutputStream dos) throws IOException {
        val port = dis.readShort();
        val ip = ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress().getAddress();
        val strInp = String.format("%s %d", Util.ipToStr(ip), port);
        log.debug("received request: UPDATE {}", strInp);
        val numFiles = dis.readInt();
        val seed = SeedRecord.builder().ip(ip).port(port).build();
        val time = System.currentTimeMillis();
        val files = new HashSet<Integer>(numFiles);
        for (int i = 0; i < numFiles; i++) {
            files.add(dis.readInt());
        }
        val client = ClientRecord.builder().files(files).lastUpdateTime(time).build();

        activeClients.put(seed, client);

        dos.writeBoolean(true);
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
            dos.writeShort(port);
        }

    }

    private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
        val name = dis.readUTF();
        val size = dis.readLong();
        log.debug("received request: UPLOAD {} {}", name, size);
        while (true) {
            val id = freeId.getAndIncrement();
            final TrackerFileRecord trackerFileRecord = TrackerFileRecord.builder().name(name).size(size).id(id).build();
            if (availableFiles.putIfAbsent(id, trackerFileRecord) == null) {
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
