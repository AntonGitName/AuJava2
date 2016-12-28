package ru.mit.spbau.antonpp.ftp.server;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.ftp.protocol.RequestCode;
import ru.mit.spbau.antonpp.ftp.server.exceptions.ConnectionException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * @author antonpp
 * @since 29/10/2016
 */
@Slf4j
public class ConnectionHandler implements Runnable {

    private final Socket clientSocket;
    private boolean isRunning = true;

    public ConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        log.debug("Connected");
        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
             DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
            clientSocket.setSoTimeout(10000);
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                handle(dis, dos);
            }
            log.debug("Handler was interrupted: {}", Thread.currentThread().isInterrupted());
        } catch (IOException e) {
            throw new ConnectionException("Could not open I/O streams", e);
        } finally {
            log.debug("Disconnected");
        }
    }

    private void handle(DataInputStream dis, DataOutputStream dos) {
        try {
            val requestCode = dis.readInt();
            log.debug("Received request with code {}", requestCode);
            switch (requestCode) {
                case RequestCode.RQ_GET:
                    handleGet(dis, dos);
                    break;
                case RequestCode.RQ_LIST:
                    handleList(dis, dos);
                    break;
                case RequestCode.RQ_DISCONNECT:
                    isRunning = false;
                    break;
                default:
                    throw new ConnectionException("Unknown command");
            }
            log.debug("Request handled");
        } catch (SocketTimeoutException e) {
            log.debug("Socket read time limit exceeded", e);
        } catch (IOException e) {
            throw new ConnectionException("Failed to handle request", e);
        }
    }

    private void handleGet(DataInputStream dis, DataOutputStream dos) throws IOException {
        val path = Paths.get(dis.readUTF());
        log.info("GET {}", path);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            log.debug("{} is not a file. Result size 0", path);
            dos.writeLong(0);
        } else {
            val fileSz = Files.size(path);
            log.debug("{} exists. Result size {}", path, fileSz);
            dos.writeLong(fileSz);
            dos.write(Files.readAllBytes(path));
        }
    }

    private void handleList(DataInputStream dis, DataOutputStream dos) throws IOException {
        val data = dis.readUTF();
        log.info("LIST {}", data);
        Path path = Paths.get(data);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            dos.writeInt(0);
        } else {
            val files = Files.list(path).sorted().collect(Collectors.toList());
            dos.writeInt(files.size());
            for (Path p : files) {
                dos.writeUTF(p.getFileName().toString());
                dos.writeBoolean(Files.isDirectory(p));
            }
        }
    }

}
