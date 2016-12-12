package ru.mit.spbau.antonpp.torrent.client.requester;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.protocol.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.protocol.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.protocol.network.ConnectionException;
import ru.mit.spbau.antonpp.torrent.protocol.protocol.ClientRequestCode;
import ru.mit.spbau.antonpp.torrent.protocol.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static ru.mit.spbau.antonpp.torrent.client.TorrentClient.MAX_THREADS;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
@Slf4j
public class ClientRequester {

    private final String host;
    private final int trackerPort;

    public ClientRequester(String host, int trackerPort) {
        this.host = host;
        this.trackerPort = trackerPort;
    }

    public Map<Integer, FileRecord> requestFilesList() throws RequestFailedException {
        return new AbstractRequester<Map<Integer, FileRecord>>(host, trackerPort) {

            @Override
            protected Map<Integer, FileRecord> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                val result = new HashMap<Integer, FileRecord>();
                val numFiles = inputStream.readInt();
                for (int i = 0; i < numFiles; ++i) {
                    val id = inputStream.readInt();
                    result.put(id, FileRecord.builder()
                            .name(inputStream.readUTF())
                            .size(inputStream.readLong())
                            .build());
                }
                return result;
            }
        }.request();
    }

    public int requestUploadFile(final String name, final long size) throws RequestFailedException {
        return new AbstractRequester<Integer>(host, trackerPort) {

            @Override
            protected Integer execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeInt(TrackerRequestCode.RQ_UPLOAD);
                outputStream.writeUTF(name);
                outputStream.writeLong(size);
                return inputStream.readInt();
            }
        }.request();
    }

    private List<SeedRecord> requestSources(int id) throws RequestFailedException {
        return new AbstractRequester<List<SeedRecord>>(host, trackerPort) {

            @Override
            protected List<SeedRecord> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeInt(TrackerRequestCode.RQ_SOURCES);
                outputStream.writeInt(id);
                val result = new ArrayList<SeedRecord>();
                val numFiles = inputStream.readInt();
                val ip = new byte[4];
                for (int i = 0; i < numFiles; ++i) {
                    ip[0] = inputStream.readByte();
                    ip[1] = inputStream.readByte();
                    ip[2] = inputStream.readByte();
                    ip[3] = inputStream.readByte();
                    result.add(SeedRecord.builder()
                            .ip(ip)
                            .port(inputStream.readShort())
                            .build());
                }
                return result;
            }
        }.request();
    }

    public void requestDownloadFileAsync(int id, ClientFileManager fileManager, DownloadFileCallback callback) {

        val executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            final Map<Integer, FileRecord> fileRecords;
            try {
                fileRecords = requestFilesList();
            } catch (RequestFailedException e) {
                callback.onFail(id, e);
                return;
            }
            val fileSize = fileRecords.entrySet().stream().filter(x -> x.getKey() == id).findAny()
                    .map(Map.Entry::getValue).map(FileRecord::getSize).orElse(0L);

            while (fileSize > fileManager.getSize(id)) {
                val alreadyHave = fileManager.getAvailableParts(id);
                try {
                    val sources = requestSources(id);
                    val sourcesMap = getSourcesMap(id, alreadyHave, sources);
                    downloadParts(id, sourcesMap, fileManager);
                } catch (RequestFailedException e) {
                    callback.onFail(id, e);
                }
            }
            callback.onFinish(id);
        });
        executor.shutdown();
    }

    private byte[] requestFilePart(int id, int part, SeedRecord seed) throws RequestFailedException {
        val ip = seed.getIp();
        val port = seed.getPort();
        return new AbstractRequester<byte[]>(ip, port) {

            @Override
            protected byte[] execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeInt(ClientRequestCode.RQ_GET);
                outputStream.writeInt(id);
                outputStream.writeInt(part);
                int size = (int) inputStream.readLong();
                val data = new byte[size];
                int read = 0;
                while (read < size) {
                    read += inputStream.read(data, read, size - read);
                }
                return data;
            }
        }.request();
    }

    private void downloadParts(int id, Map<Integer, SeedRecord> sourcesMap, ClientFileManager fileManager) {
        val executor = Executors.newFixedThreadPool(MAX_THREADS);
        sourcesMap.entrySet().forEach(x -> {
            val part = x.getKey();
            val seed = x.getValue();
            executor.execute(() -> {
                try {
                    val data = requestFilePart(id, part, seed);
                    fileManager.updateFilePart(id, part, data);
                } catch (RequestFailedException | IOException e) {
                    log.error("Failed to download part from {}", seed, e);
                }
            });
        });
        executor.shutdown();
    }

    private Set<Integer> requestSeedAvailableParts(int id, SeedRecord seedRecord) throws RequestFailedException {
        return new AbstractRequester<Set<Integer>>(seedRecord.getIp(), seedRecord.getPort()) {
            @Override
            protected Set<Integer> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeInt(ClientRequestCode.RQ_STAT);
                outputStream.writeInt(id);
                val result = new HashSet<Integer>();
                val num = inputStream.readInt();
                for (int i = 0; i < num; ++i) {
                    result.add(inputStream.readInt());
                }
                return result;
            }
        }.request();
    }

    private Map<Integer, SeedRecord> getSourcesMap(int id, Set<Integer> notNeededParts, List<SeedRecord> seeds) {
        val sourcesMap = new ConcurrentHashMap<Integer, SeedRecord>();
        val executor = Executors.newFixedThreadPool(MAX_THREADS);
        seeds.forEach(seed -> {
            executor.execute(() -> {
                try {
                    val availableParts = requestSeedAvailableParts(id, seed);
                    availableParts.removeAll(notNeededParts);
                    availableParts.forEach(x -> {
                        sourcesMap.putIfAbsent(x, seed);
                    });
                } catch (RequestFailedException | ConnectionException e) {
                    log.error("Connection to {} failed. Ignoring it.", seed, e);
                }
            });
        });
        executor.shutdown();
        return sourcesMap;
    }

    public interface DownloadFileCallback {
        void onFinish(int id);

        void onFail(int id, Throwable e);
    }
}
