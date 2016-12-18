package ru.mit.spbau.antonpp.ftp.server;

import lombok.extern.slf4j.Slf4j;
import ru.mit.spbau.antonpp.ftp.server.exceptions.FtpServerException;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class organises the work process of the server.
 *
 * @author antonpp
 * @since 29/10/2016
 */
@Slf4j
public class FtpServer implements Closeable {

    static final int MAX_THREADS = 4;
    private final int port;
    private ExecutorService listenService;
    private PortListener portListener;
    private boolean isStarted = true;

    /**
     * Creates server socket on the specified port.
     *
     * @param port port to be listened when the server is started.
     */
    public FtpServer(int port) {
        this.port = port;
    }

    /**
     * Creates a thread that is used for listening servers's port.
     *
     * @throws FtpServerException if ServerSocket could not be created with specified port.
     */
    public void start() throws FtpServerException {
        final ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new FtpServerException("Could not start server", e);
        }
        portListener = new PortListener(serverSocket);
        listenService = Executors.newSingleThreadExecutor();
        listenService.execute(portListener);
        isStarted = true;
    }

    /**
     * Terminates server by shut downing the thread with listener of the port.
     *
     * @throws IOException if socket could not be closed correctly
     */
    public void stop() throws IOException {
        if (isStarted) {
            portListener.stop();
            listenService.shutdown();
            portListener = null;
            listenService = null;
        } else {
            throw new IllegalStateException("Cannot stop not started server");
        }

    }

    @Override
    public void close() throws IOException {
        if (isStarted) {
            stop();
        }
    }
}
