package ru.mit.spbau.antonpp.torrent.client.updater;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.ClientConnectionException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.protocol.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
            val clientSocket = new Socket(host, trackerPort);
            val inputStream = new DataInputStream(clientSocket.getInputStream());
            val outputStream = new DataOutputStream(clientSocket.getOutputStream());
            log.debug("connected");

            outputStream.writeInt(TrackerRequestCode.RQ_UPDATE);
            outputStream.writeShort(clientPort);

            val availableFiles = fileManager.getAvailableFiles();
            outputStream.writeInt(availableFiles.size());
            for (final int availableFile : availableFiles) {
                outputStream.writeInt(availableFile);
            }

            val status = inputStream.readBoolean();
            log.info("Client send update to tracker, status: {}", status);

            outputStream.close();
            inputStream.close();
            clientSocket.close();
            log.debug("disconnected");
        } catch (IOException e) {
            throw new ClientConnectionException("Could not connect to specified host", e);
        }

    }
}
