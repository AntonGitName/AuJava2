package ru.mit.spbau.antonpp.torrent.client.uploader;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.commons.network.AbstractConnectionHandler;
import ru.mit.spbau.antonpp.torrent.commons.network.ConnectionIOException;
import ru.mit.spbau.antonpp.torrent.commons.protocol.ClientRequestCode;
import ru.mit.spbau.antonpp.torrent.commons.protocol.CommonRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class UploaderConnectionHandler extends AbstractConnectionHandler {

    @NotNull
    private final ClientFileManager fileManager;

    public UploaderConnectionHandler(Socket clientSocket, @NotNull ClientFileManager fileManager) {
        super(clientSocket);
        this.fileManager = fileManager;
    }

    @Override
    protected void onConnected() {
        log.debug("Connected");
    }

    @Override
    protected void onDisconnected() {
        log.debug("Disconnected");
    }

    protected void handle(byte requestCode, DataInputStream dis, DataOutputStream dos) {
        try {
            switch (requestCode) {
                case ClientRequestCode.RQ_GET:
                    handleGet(dis, dos);
                    break;
                case ClientRequestCode.RQ_STAT:
                    handleStat(dis, dos);
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

    private void handleGet(DataInputStream dis, DataOutputStream dos) throws IOException {
        val id = dis.readInt();
        val part = dis.readInt();
        log.debug("received request: GET {} {}", id, part);
        val data = fileManager.getFilePart(id, part);
        dos.writeLong(data.length);
        dos.write(data);
    }

    private void handleStat(DataInputStream dis, DataOutputStream dos) throws IOException {
        val id = dis.readInt();
        log.debug("received request: STAT {}", id);
        val availableParts = fileManager.getAvailableParts(id);
        dos.writeInt(availableParts.size());
        for (final int availablePart : availableParts) {
            dos.writeInt(availablePart);
        }
    }

}
