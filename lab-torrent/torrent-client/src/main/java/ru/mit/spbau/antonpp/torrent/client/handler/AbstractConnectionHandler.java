package ru.mit.spbau.antonpp.torrent.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.torrent.client.exceptions.ClientConnectionException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public abstract class AbstractConnectionHandler implements Runnable {

    private final Socket clientSocket;
    @NotNull
    protected final ClientFileManager fileManager;

    public AbstractConnectionHandler(Socket clientSocket, @NotNull ClientFileManager fileManager) {
        this.clientSocket = clientSocket;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        log.debug("Connected");
        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
            clientSocket.setSoTimeout(10000);
            handle(dis, dos);
        } catch (IOException e) {
            throw new ClientConnectionException("Could not open I/O streams", e);
        } finally {
            log.debug("Disconnected");
        }
    }

    protected abstract void handle(DataInputStream dis, DataOutputStream dos);
}
