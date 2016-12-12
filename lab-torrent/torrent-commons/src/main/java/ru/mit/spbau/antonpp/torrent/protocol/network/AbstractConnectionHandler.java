package ru.mit.spbau.antonpp.torrent.protocol.network;

import lombok.extern.slf4j.Slf4j;

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

    public AbstractConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        log.debug("Connected");
        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
            clientSocket.setSoTimeout(10000);
            handle(dis, dos);
        } catch (IOException e) {
            throw new ConnectionException("Could not open I/O streams", e);
        } finally {
            log.debug("Disconnected");
        }
    }

    protected abstract void handle(DataInputStream dis, DataOutputStream dos);
}
