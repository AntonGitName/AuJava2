package ru.mit.spbau.antonpp.ftp.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.ftp.client.exceptions.FtpClientException;
import ru.mit.spbau.antonpp.ftp.protocol.RequestCode;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author antonpp
 * @since 30/10/2016
 */
@Slf4j
public class FtpClient implements Closeable {

    private final Socket clientSocket;

    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    public FtpClient(String host, int port) throws FtpClientException {
        try {
            clientSocket = new Socket(host, port);
            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            throw new FtpClientException("Could not connect to specified host", e);
        }
        log.debug("connected");
    }

    public Map<String, Boolean> listFiles(String dir) throws FtpClientException {
        try {
            val result = new HashMap<String, Boolean>();
            outputStream.writeInt(RequestCode.RQ_LIST);
            outputStream.writeUTF(dir);
            val len = inputStream.readInt();
            log.debug("Received list length: {}", len);
            for (int i = 0; i < len; i++) {
                result.put(inputStream.readUTF(), inputStream.readBoolean());
            }
            return result;
        } catch (IOException e) {
            throw new FtpClientException("Failed to execute LIST", e);
        }
    }

    public byte[] getFile(String path) throws FtpClientException {
        try {
            outputStream.writeInt(RequestCode.RQ_GET);
            outputStream.writeUTF(path);
            val len = inputStream.readLong();
            log.debug("Received file size: {}", len);
            byte[] data = new byte[(int) len];
            inputStream.readFully(data);
            return data;
        } catch (IOException e) {
            throw new FtpClientException("Failed to execute GET", e);
        }
    }

    @Override
    public void close() throws IOException {
        outputStream.writeInt(RequestCode.RQ_DISCONNECT);
        outputStream.close();
        inputStream.close();
        clientSocket.close();
        log.debug("disconnected");
    }
}
