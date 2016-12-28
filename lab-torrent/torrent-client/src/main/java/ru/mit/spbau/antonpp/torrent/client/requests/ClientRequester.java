package ru.mit.spbau.antonpp.torrent.client.requests;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientStartException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
@Slf4j
public class ClientRequester implements Closeable {

    static final int MAX_THREADS_PARTS = 4;
    private static final int MAX_THREADS_FILES = 2;
    private final String host;
    private final int trackerPort;
    @Getter
    private final ExecutorService uploadPartsExecutor = Executors.newFixedThreadPool(MAX_THREADS_PARTS);
    private final ExecutorService uploadFileExecutor = Executors.newFixedThreadPool(MAX_THREADS_FILES);

    public ClientRequester(String host, int trackerPort) throws TorrentClientStartException {
        this.host = host;
        this.trackerPort = trackerPort;
    }

    List<SeedRecord> requestSources(int id) throws RequestFailedException {
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
                    int port = inputStream.readShort();
                    if (port < 0) {
                        port += 2 << 15;
                    }

                    result.add(SeedRecord.builder()
                            .ip(ip)
                            .port(port)
                            .build());
                }
                return result;
            }
        }.request();
    }

    public Map<Integer, TrackerFileRecord> requestFilesList() throws RequestFailedException {
        return new AbstractRequester<Map<Integer, TrackerFileRecord>>(host, trackerPort) {

            @Override
            protected Map<Integer, TrackerFileRecord> execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeByte(TrackerRequestCode.RQ_LIST);
                val result = new HashMap<Integer, TrackerFileRecord>();
                val numFiles = inputStream.readInt();
                for (int i = 0; i < numFiles; ++i) {
                    val id = inputStream.readInt();
                    result.put(id, TrackerFileRecord.builder()
                            .id(id)
                            .name(inputStream.readUTF())
                            .size(inputStream.readLong())
                            .build());
                }
                return result;
            }
        }.request();
    }

    public TrackerFileRecord requestUploadFile(final String name, final long size) throws RequestFailedException {
        return new AbstractRequester<TrackerFileRecord>(host, trackerPort) {

            @Override
            protected TrackerFileRecord execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                outputStream.writeByte(TrackerRequestCode.RQ_UPLOAD);
                outputStream.writeUTF(name);
                outputStream.writeLong(size);
                return TrackerFileRecord.builder().name(name).size(size).id(inputStream.readInt()).build();
            }
        }.request();
    }

    public void requestDownloadFile(int id, ClientFileManager fileManager, DownloadFileCallback callback) {
        uploadFileExecutor.execute(new DownloadFileTask(id, callback, this, fileManager));
    }

    @Override
    public void close() {
        uploadPartsExecutor.shutdownNow();
        uploadFileExecutor.shutdownNow();
    }

}
