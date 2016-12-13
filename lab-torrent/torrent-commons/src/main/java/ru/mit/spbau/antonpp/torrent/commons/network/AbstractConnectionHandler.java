package ru.mit.spbau.antonpp.torrent.commons.network;

import lombok.val;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public abstract class AbstractConnectionHandler implements Runnable {

    protected final Socket clientSocket;
    private boolean isConnected;

    public AbstractConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    protected abstract void onConnected();

    protected abstract void onDisconnected();

    @Override
    public void run() {
        isConnected = true;
        onConnected();
        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
            clientSocket.setSoTimeout(10000);
            while (isConnected) {
                val requestCode = dis.readByte();
                handle(requestCode, dis, dos);
            }
        } catch (IOException e) {
            throw new ConnectionException("Could not open I/O streams", e);
        } finally {
            onDisconnected();
        }
    }

    protected abstract void handle(byte requestCode, DataInputStream dis, DataOutputStream dos);

    protected void disconnect() {
        isConnected = false;
    }
}
