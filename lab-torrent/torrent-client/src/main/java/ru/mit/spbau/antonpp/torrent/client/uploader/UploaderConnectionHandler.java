package ru.mit.spbau.antonpp.torrent.client.uploader;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.exceptions.ClientConnectionException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;
import ru.mit.spbau.antonpp.torrent.protocol.network.AbstractConnectionHandler;
import ru.mit.spbau.antonpp.torrent.protocol.protocol.ClientRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

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

    protected void handle(DataInputStream dis, DataOutputStream dos) {
        try {
            val requestCode = dis.readInt();
            log.debug("Received request with code {}", requestCode);
            switch (requestCode) {
                case ClientRequestCode.RQ_GET:
                    handleGet(dis, dos);
                    break;
                case ClientRequestCode.RQ_STAT:
                    handleStat(dis, dos);
                    break;
                default:
                    throw new ClientConnectionException("Unknown command");
            }
            log.debug("Request handled");
        } catch (SocketTimeoutException e) {
            log.debug("Socket read time limit exceeded", e);
        } catch (IOException e) {
            throw new ClientConnectionException("Failed to handle request", e);
        }
    }

    private void handleGet(DataInputStream dis, DataOutputStream dos) throws IOException {
        val id = dis.readInt();
        val part = dis.readInt();
        log.debug("received request: GET {} {}", id, part);
        dos.write(fileManager.getFilePart(id, part));
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
