package ru.mit.spbau.antonpp.torrent.client.requester;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.ClientConnectionException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.protocol.ClientRequestCode;
import ru.mit.spbau.antonpp.torrent.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    public List<FileRecord> requestFilesList() throws RequestFailedException {
        return new AbstractRequester<List<FileRecord>>(host, trackerPort) {

            @Override
            protected List<FileRecord> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                val result = new ArrayList<FileRecord>();
                val numFiles = inputStream.readInt();
                for (int i = 0; i < numFiles; ++i) {
                    result.add(FileRecord.builder()
                            .id(inputStream.readInt())
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

    public void requestDownloadFileAsync(int id, ClientFileManager fileManager) throws RequestFailedException {
        val fileSize = requestFilesList().stream().filter(x -> x.getId() == id).findAny()
                .map(FileRecord::getSize).orElse(0L);

        while (fileSize > fileManager.getSize(id)) {
            val sources = new AbstractRequester<List<SeedRecord>>(host, trackerPort) {

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
                                .port(inputStream.readInt())
                                .build());
                    }
                    return result;
                }
            }.request();

            val alreadyHave = fileManager.getAvailableParts(id);
            try {
                val sourcesMap = getSourcesMap(id, alreadyHave, sources);
                downloadParts(id, sourcesMap, fileManager);
            } catch (InterruptedException e) {
                throw new RequestFailedException(e);
            }
        }
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

    private void downloadParts(int id, Map<Integer, SeedRecord> sourcesMap, ClientFileManager fileManager)
            throws InterruptedException {
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
        executor.awaitTermination(2, TimeUnit.MINUTES);
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

    private Map<Integer, SeedRecord> getSourcesMap(int id, Set<Integer> notNeededParts, List<SeedRecord> seeds)
            throws InterruptedException {
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
                } catch (RequestFailedException | ClientConnectionException e) {
                    log.error("Connection to {} failed. Ignoring it.", seed, e);
                }
            });
        });
        executor.awaitTermination(2, TimeUnit.MINUTES);
        executor.shutdown();
        return sourcesMap;
    }


    @ToString
    @Builder
    private static final class SeedRecord {
        @Getter
        private final byte[] ip;
        @Getter
        private final int port;
    }


    @Builder
    public static final class FileRecord {
        @Getter
        private final String name;
        @Getter
        private final int id;
        @Getter
        private final long size;
    }
}
