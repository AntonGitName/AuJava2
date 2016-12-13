package ru.mit.spbau.antonpp.torrent.client.requests;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TrackerUpdateFailedException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class ClientStatusUpdater implements Runnable {
    private final String host;
    private final short trackerPort;
    private final short clientPort;
    private final ClientFileManager fileManager;


    public ClientStatusUpdater(String host, short trackerPort, ClientFileManager fileManager, short clientPort) {
        this.host = host;
        this.trackerPort = trackerPort;
        this.fileManager = fileManager;
        this.clientPort = clientPort;

    }

    @Override
    public void run() {
        try {
            new AbstractRequester<Boolean>(host, trackerPort) {

                @Override
                protected Boolean execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
                    outputStream.writeByte(TrackerRequestCode.RQ_UPDATE);
                    outputStream.writeShort(clientPort);

                    val availableFiles = fileManager.getAvailableFiles();
                    outputStream.writeInt(availableFiles.size());
                    for (final int availableFile : availableFiles) {
                        outputStream.writeInt(availableFile);
                    }

                    val status = inputStream.readBoolean();
                    log.info("Client send update to tracker, response: {}", status);

                    return status;
                }
            }.request();
        } catch (RequestFailedException e) {
            throw new TrackerUpdateFailedException(e);
        }
    }
}
