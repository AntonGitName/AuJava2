package ru.mit.spbau.antonpp.torrent.client.requests;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.NoSuchFileException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;
import ru.mit.spbau.antonpp.torrent.commons.network.ConnectionIOException;
import ru.mit.spbau.antonpp.torrent.commons.protocol.ClientRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static ru.mit.spbau.antonpp.torrent.client.requests.ClientRequester.MAX_THREADS_PARTS;

/**
 * @author antonpp
 * @since 14/12/2016
 */
@Slf4j
public class DownloadFileTask implements Runnable {

    private final int id;
    private final DownloadFileCallback callback;
    private final ClientRequester requester;
    private final ClientFileManager fileManager;

    public DownloadFileTask(int id, DownloadFileCallback callback, ClientRequester requester, ClientFileManager fileManager) {
        this.id = id;
        this.callback = callback;
        this.requester = requester;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        try {
            final Map<Integer, TrackerFileRecord> fileRecords = requester.requestFilesList();
            if (!fileRecords.containsKey(id)) {
                callback.onFail(id, new NoSuchFileException(id));
            }
            val record = fileRecords.get(id);
            if (!fileManager.hasFile(id)) {
                fileManager.createEmpty(record);
            }

            long fileSizeClient = fileManager.getSize(id);
            callback.progress(id, fileSizeClient, record.getSize());

            while (fileSizeClient < record.getSize()) {
                val alreadyHave = fileManager.getAvailableParts(id);
                val sources = requester.requestSources(id);
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
        } catch (RequestFailedException | IOException e) {
            callback.onFail(id, e);
            return;
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
        val partFutures = sourcesMap.entrySet().stream().limit(MAX_THREADS_PARTS).map(x -> requester.getUploadPartsExecutor().submit(() -> {
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
}
