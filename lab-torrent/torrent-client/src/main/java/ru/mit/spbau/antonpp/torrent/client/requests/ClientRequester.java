package ru.mit.spbau.antonpp.torrent.client.requests;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.network.ConnectionIOException;
import ru.mit.spbau.antonpp.torrent.commons.protocol.ClientRequestCode;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.mit.spbau.antonpp.torrent.client.TorrentClient.MAX_THREADS;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
@Slf4j
public class ClientRequester implements Closeable {

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
                            .id(id)
                            .name(inputStream.readUTF())
                            .size(inputStream.readLong())
                            .build());
                }
                return result;
            }
        }.request();
    }

    public FileRecord requestUploadFile(final String name, final long size) throws RequestFailedException {
        return new AbstractRequester<FileRecord>(host, trackerPort) {

            @Override
            protected FileRecord execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeByte(TrackerRequestCode.RQ_UPLOAD);
                outputStream.writeUTF(name);
                outputStream.writeLong(size);
                return FileRecord.builder().name(name).size(size).id(inputStream.readInt()).build();
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

    public void requestDownloadFile(int id, ClientFileManager fileManager, DownloadFileCallback callback) throws RequestFailedException {
        final Map<Integer, FileRecord> fileRecords = requestFilesList();
        if (!fileRecords.containsKey(id)) {
            throw new RequestFailedException("No files were found with such id");
        }
        val record = fileRecords.get(id);
        try {
            if (!fileManager.hasFile(id)) {
                fileManager.createEmpty(record);
            }

            long fileSizeClient = fileManager.getSize(id);
            callback.progress(id, fileSizeClient, record.getSize());

            while (fileSizeClient < record.getSize()) {
                val alreadyHave = fileManager.getAvailableParts(id);
                val sources = requestSources(id);
                val sourcesMap = getSourcesMap(id, alreadyHave, sources);

                if (sourcesMap.isEmpty()) {
                    // there are alive seeds, but they do not have any new parts
                    callback.noSeeds(id);
                    return;
                }

                downloadParts(id, sourcesMap, fileManager);
                fileSizeClient = fileManager.getSize(id);
                callback.progress(id, fileSizeClient, record.getSize());
            }
        } catch (IOException e) {
            throw new RequestFailedException(e);
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
        val partFutures = sourcesMap.entrySet().stream().limit(MAX_THREADS).map(x -> uploadExecutor.submit(() -> {
            final int part = x.getKey();
            final SeedRecord seed = x.getValue();
            try {
                final byte[] data = requestFilePart(id, part, seed);
                fileManager.updateFilePart(id, part, data);
            } catch (RequestFailedException | IOException e) {
                throw new RuntimeException(e);
            }
        })).collect(Collectors.toList());

        Throwable lastError = null;
        boolean atLeastOneFinished = false;

        for (val partFuture : partFutures) {
            try {
                partFuture.get();
                atLeastOneFinished = true;
            } catch (ExecutionException | InterruptedException e) {
                // ignore it as we do not want to cancel downloading because of one failed part
                lastError = e;
            }
        }
        if (!atLeastOneFinished) {
            // no parts downloaded - something really wrong
            throw new RequestFailedException("Could not complete downloading of any part", lastError);
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

    private Map<Integer, SeedRecord> getSourcesMap(int id, Set<Integer> notNeededParts, List<SeedRecord> seeds) throws RequestFailedException {
        val sourcesMap = new HashMap<Integer, List<SeedRecord>>();
        Throwable lastError = null;
        boolean allSeedsFailed = true;
        for (val seed : seeds) {
            try {
                val availableParts = requestSeedAvailableParts(id, seed);
                availableParts.removeAll(notNeededParts);
                availableParts.forEach(x -> {
                    if (!sourcesMap.containsKey(x)) {
                        sourcesMap.put(x, new ArrayList<>());
                    }
                    sourcesMap.get(x).add(seed);
                });
                allSeedsFailed = false;
            } catch (ConnectionIOException e) {
                log.error("Connection to {} failed. Ignoring this seed.", seed, e);
                lastError = e;
            }
        }
        if (allSeedsFailed) {
            // could not connect to any seed. Should throw the reason
            throw new RequestFailedException("Could not connect to any seed", lastError);
        }
        return sourcesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, x -> Util.getRandomElement(x.getValue())));
    }

    @Override
    public void close() throws IOException {
        uploadExecutor.shutdownNow();
    }

    public interface DownloadFileCallback {
        void onFinish(int id);

        void onFail(int id, Throwable e);

        void progress(int id, long downloadedSize, long fullSize);

        void noSeeds(int id);

    }
}
