package ru.mit.spbau.antonpp.torrent.client.requests;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.network.ConnectionException;
import ru.mit.spbau.antonpp.torrent.commons.protocol.ClientRequestCode;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static ru.mit.spbau.antonpp.torrent.client.TorrentClient.MAX_THREADS;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
@Slf4j
public class ClientRequester {

    private final String host;
    private final int trackerPort;
    private final ExecutorService uploadExecutor = Executors.newFixedThreadPool(MAX_THREADS);

    public ClientRequester(String host, int trackerPort) {
        this.host = host;
        this.trackerPort = trackerPort;
    }

    public Map<Integer, FileRecord> requestFilesList() throws RequestFailedException {
        return new AbstractRequester<Map<Integer, FileRecord>>(host, trackerPort) {

            @Override
            protected Map<Integer, FileRecord> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeByte(TrackerRequestCode.RQ_LIST);
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
                outputStream.writeByte(TrackerRequestCode.RQ_UPLOAD);
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
                outputStream.writeByte(TrackerRequestCode.RQ_SOURCES);
                outputStream.writeInt(id);
                val result = new ArrayList<SeedRecord>();
                val numFiles = inputStream.readInt();
                val ip = new byte[4];
                for (int i = 0; i < numFiles; ++i) {
                    for (int j = 0; j < 4; ++j) {
                        ip[j] = inputStream.readByte();
                    }
                    result.add(SeedRecord.builder()
                            .ip(ip)
                            .port(inputStream.readShort())
                            .build());
                }
                return result;
            }
        }.request();
    }

    public void requestDownloadFile(int id, ClientFileManager fileManager, DownloadFileCallback callback) {
        final Map<Integer, FileRecord> fileRecords;
        try {
            fileRecords = requestFilesList();
        } catch (RequestFailedException e) {
            callback.onFail(id, e);
            return;
        }
        final long fileSizeTracker = fileRecords.entrySet().stream().filter(x -> x.getKey() == id).findAny()
                .map(Map.Entry::getValue).map(FileRecord::getSize).orElse(0L);

        while (true) {
            final long fileSizeClient;
            try {
                fileSizeClient = fileManager.getSize(id);
                if (fileSizeClient >= fileSizeTracker) {
                    break;
                }
                callback.progress(id, fileSizeClient, fileSizeTracker);
                final Set<Integer> alreadyHave = fileManager.getAvailableParts(id);
                final List<SeedRecord> sources = requestSources(id);
                final Map<Integer, SeedRecord> sourcesMap = getSourcesMap(id, alreadyHave, sources);
                downloadParts(id, sourcesMap, fileManager);
            } catch (RequestFailedException | IOException | InterruptedException e) {
                callback.onFail(id, e);
            }
        }
        callback.onFinish(id);
    }

    private byte[] requestFilePart(int id, int part, SeedRecord seed) throws RequestFailedException {
        val ip = seed.getIp();
        val port = seed.getPort();
        return new AbstractRequester<byte[]>(ip, port) {

            @Override
            protected byte[] execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeByte(ClientRequestCode.RQ_GET);
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

    private void downloadParts(int id, Map<Integer, SeedRecord> sourcesMap, ClientFileManager fileManager) throws RequestFailedException {
        // fuck lombok + idea + java 1.8
        // it seems that val does not work when it is declared in lambda scope

        List<? extends Future<?>> partFutures = sourcesMap.entrySet().stream().map(x -> uploadExecutor.submit(() -> {
            final int part = x.getKey();
            final SeedRecord seed = x.getValue();
            try {

                final byte[] data = requestFilePart(id, part, seed);
                fileManager.updateFilePart(id, part, data);
            } catch (RequestFailedException | IOException e) {
                log.error("Failed to download part from {}", seed, e);
            }
        })).collect(Collectors.toList());
        for (val partFuture : partFutures) {
            try {
                partFuture.get();
            } catch (ExecutionException e) {
                // ignore it as we do not want to cancel downloading because of one failed part
            } catch (InterruptedException e) {
                throw new RequestFailedException(e);
            }
        }

    }

    private Set<Integer> requestSeedAvailableParts(int id, SeedRecord seedRecord) throws RequestFailedException {
        return new AbstractRequester<Set<Integer>>(seedRecord.getIp(), seedRecord.getPort()) {
            @Override
            protected Set<Integer> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeByte(ClientRequestCode.RQ_STAT);
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

    private Map<Integer, SeedRecord> getSourcesMap(int id, Set<Integer> notNeededParts, List<SeedRecord> seeds) throws InterruptedException {
        val sourcesMap = new HashMap<Integer, List<SeedRecord>>();
        for (val seed : seeds) {
            try {
                final Set<Integer> availableParts = requestSeedAvailableParts(id, seed);
                availableParts.removeAll(notNeededParts);
                availableParts.forEach(x -> {
                    if (!sourcesMap.containsKey(x)) {
                        sourcesMap.put(x, new ArrayList<>());
                    }
                    sourcesMap.get(x).add(seed);
                });
            } catch (RequestFailedException | ConnectionException e) {
                log.error("Connection to {} failed. Ignoring this seed.", seed, e);
            }
        }
        return sourcesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, x -> Util.getRandomElement(x.getValue())));
    }

    public interface DownloadFileCallback {
        void onFinish(int id);

        void onFail(int id, Throwable e);

        void progress(int id, long downloadedSize, long fullSize);

    }
}
