package ru.mit.spbau.antonpp.torrent.client.requests;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.ClientConnectionException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.commons.protocol.CommonRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Anton Mordberg
 * @since 12.12.16
 */
@Slf4j
public abstract class AbstractRequester<T> {
    private final InetAddress address;
    private final int trackerPort;

    AbstractRequester(String host, int trackerPort) throws RequestFailedException {
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RequestFailedException(e);
        }
        this.trackerPort = trackerPort;
    }

    AbstractRequester(byte[] ip, int trackerPort) throws RequestFailedException {
        try {
            address = InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            throw new RequestFailedException(e);
        }
        this.trackerPort = trackerPort;
    }

    protected abstract T execute(DataInputStream inputStream, DataOutputStream outputStream) throws IOException;

    T request() throws ClientConnectionException {
        final T result;
        try {
            val clientSocket = new Socket(address, trackerPort);
            val inputStream = new DataInputStream(clientSocket.getInputStream());
            val outputStream = new DataOutputStream(clientSocket.getOutputStream());

            log.debug("connected");

            result = execute(inputStream, outputStream);
            outputStream.writeByte(CommonRequestCode.RQ_DC);

            outputStream.close();
            inputStream.close();
            clientSocket.close();
            log.debug("disconnected");
        } catch (IOException e) {
            throw new ClientConnectionException("Could not connect to specified host", e);
        }
        return result;
    }
}
