package ru.mit.spbau.antonpp.torrent.client;

import com.google.common.util.concurrent.FutureCallback;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.mit.spbau.antonpp.torrent.client.requests.DownloadFileCallback;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;
import ru.mit.spbau.antonpp.torrent.commons.protocol.CommonRequestCode;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * @author Anton Mordberg
 * @since 13.12.16
 */
public class TorrentClientTest {

    private static final short TEST_PORT1 = (short) 31001;
    private static final short TEST_PORT2 = (short) 31002;
    private static final short TEST_PORT3 = (short) 31003;
    private static final short TEST_PORT_TRACKER = (short) 8081;
    private static final String HOST = "localhost";
    private static final int WAIT_THREADS_TIME = 1500;
    private static final Path TEST_FILES_DIR = Paths.get(TorrentClientTest.class.getClassLoader()
            .getResource("files").getPath());
    private static final Path FILE_A1 = TEST_FILES_DIR.resolve("dir1").resolve("a.txt");
    private static Path clientWd1;
    private static Path clientWd2;
    private static Path clientWd3;
    private static Path downloadDestination;
    private static boolean oneSeed = true;
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private MockTracker tracker;
    private ExecutorService trackerExecutor = Executors.newSingleThreadExecutor();
    private StubCallback updateCallback;

    @Before
    public void setUp() throws Exception {
        final Path testDir = Paths.get(folder.getRoot().getCanonicalPath());
        clientWd1 = testDir.resolve("client1");
        clientWd2 = testDir.resolve("client2");
        clientWd3 = testDir.resolve("client3");
        downloadDestination = testDir.resolve("downloaded_file");

        updateCallback = new StubCallback();
        tracker = new MockTracker();
        trackerExecutor.execute(tracker);
        MockConnectionHandler.SEEDS.clear();
    }

    @Test
    public void testClientSendsUpdate() throws Exception {
        TorrentClient client = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);

        Thread.sleep(WAIT_THREADS_TIME);
        client.close();
        assertEquals(1, MockConnectionHandler.SEEDS.size());
        assertTrue(!updateCallback.isFailed());
    }

    @Test
    public void testCreateDir() throws Exception {
        TorrentClient client = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        Thread.sleep(WAIT_THREADS_TIME);
        client.close();
        assertTrue(Files.exists(clientWd1));
        assertTrue(!updateCallback.isFailed());
    }

    @Test
    public void testUsesSavedInstance() throws Exception {
        TorrentClient client = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        assertTrue(client.fileManager.getAvailableFiles().isEmpty());
        client.requestUploadFile(FILE_A1.toString(), FILE_A1.toString());
        Thread.sleep(WAIT_THREADS_TIME);
        client.close();

        client = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        assertTrue(!client.fileManager.getAvailableFiles().isEmpty());
        client.close();
        assertTrue(!updateCallback.isFailed());
    }

    @After
    public void tearDown() throws Exception {
        tracker.stop();
        trackerExecutor.shutdownNow();
        Thread.sleep(WAIT_THREADS_TIME);
    }

    @Test
    public void testRequestDownloadFile() throws Exception {
        oneSeed = true;
        TorrentClient client1 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        client1.requestUploadFile(FILE_A1.toString(), FILE_A1.toString());
        int id = client1.fileManager.getAvailableFiles().iterator().next();

        // to make sure client1 is first, so we will give correct ip/port to client2
        Thread.sleep(WAIT_THREADS_TIME);

        TorrentClient client2 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT2, updateCallback, clientWd2);
        DownloadCallback callback = new DownloadCallback(FILE_A1);
        client2.requestDownloadFile(id, downloadDestination.toString(), callback);
        int sleepTime = 0;
        while (!callback.isReady()) {
            int ms = 100;
            sleepTime += ms;
            if (sleepTime >= 3000) {
                fail("file should be ready by this time");
            }
            Thread.sleep(ms);
        }

        client1.close();
        client2.close();
    }

    @Test
    public void testRequestDownloadFileMultipleSeed() throws Exception {
        oneSeed = false;
        TorrentClient client1 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        client1.requestUploadFile(FILE_A1.toString(), FILE_A1.toString());

        TorrentClient client2 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT2, updateCallback, clientWd2);
        client2.requestUploadFile(FILE_A1.toString(), FILE_A1.toString());
        int id = client1.fileManager.getAvailableFiles().iterator().next();

        Thread.sleep(WAIT_THREADS_TIME);

        TorrentClient client3 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT3, updateCallback, clientWd3);
        DownloadCallback callback = new DownloadCallback(FILE_A1);
        client3.requestDownloadFile(id, downloadDestination.toString(), callback);

        client1.close();
        client2.close();
        client3.close();
    }

    @Test
    public void testClientsSendUploadAfterStart() throws Exception {
        TorrentClient client1 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        TorrentClient client2 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT2, updateCallback, clientWd1);
        Thread.sleep(WAIT_THREADS_TIME);

        client1.close();
        client2.close();

        List<SeedRecord> seeds = MockConnectionHandler.SEEDS;
        assertEquals(2, seeds.size());
        assertTrue(!updateCallback.isFailed());
    }

    @Test
    public void requestFilesList() throws Exception {
        TorrentClient client1 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);

        Map<Integer, TrackerFileRecord> result = client1.requestFilesList();

        assertEquals(1, result.size());
        assertTrue(result.containsKey(MockConnectionHandler.freeId));
        TrackerFileRecord record = result.get(MockConnectionHandler.freeId);
        assertEquals(FILE_A1.getFileName().toString(), record.getName());
        assertEquals(Files.size(FILE_A1), record.getSize());
        client1.close();
    }

    @Test
    public void requestUploadFile() throws Exception {
        TorrentClient client1 = new TorrentClient(HOST, TEST_PORT_TRACKER, TEST_PORT1, updateCallback, clientWd1);
        client1.requestUploadFile(FILE_A1.toString(), FILE_A1.toString());
        int id = client1.fileManager.getAvailableFiles().iterator().next();
        assertEquals(MockConnectionHandler.freeId, id);
        client1.close();
    }

    private static class DownloadCallback implements DownloadFileCallback {

        private final Path original;
        private boolean ready = false;

        private DownloadCallback(Path original) {
            this.original = original;
        }

        @Override
        public void onFinish(int id) {
            try {
                assertTrue(com.google.common.io.Files.equal(downloadDestination.toFile(), original.toFile()));
                ready = true;
            } catch (IOException e) {
                fail();
            }
        }

        @Override
        public void onFail(int id, Throwable e) {
            fail(e.getMessage());
        }

        @Override
        public void progress(int id, long downloadedSize, long fullSize) {
        }

        @Override
        public void noSeeds(int id) {

        }

        boolean isReady() {
            return ready;
        }
    }

    private static class MockConnectionHandler implements Runnable {

        final static List<SeedRecord> SEEDS = new ArrayList<>(2);
        static int freeId = 666;
        private final Socket clientSocket;
        private boolean isConnected;

        MockConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            isConnected = true;
            try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                 DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {
                clientSocket.setSoTimeout(64000);
                while (isConnected) {
                    byte requestCode = dis.readByte();
                    switch (requestCode) {
                        case CommonRequestCode.RQ_DC:
                            isConnected = false;
                            break;
                        case TrackerRequestCode.RQ_UPDATE:
                            handleUpdate(dis, dos);
                            break;
                        case TrackerRequestCode.RQ_UPLOAD:
                            handleUpload(dis, dos);
                            break;
                        case TrackerRequestCode.RQ_SOURCES:
                            handleSources(dis, dos);
                            break;
                        case TrackerRequestCode.RQ_LIST:
                            handleList(dis, dos);
                            break;
                        default:
                            fail();
                    }
                }
            } catch (IOException e) {
                fail();
            }
        }

        private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
            dis.readUTF();
            dis.readLong();

            dos.writeInt(freeId);
        }

        private void handleUpdate(DataInputStream dis, DataOutputStream dos) throws IOException {
            // we dont care about input
            short port = dis.readShort();
            int numFiles = dis.readInt();
            for (int i = 0; i < numFiles; i++) {
                dis.readInt();
            }

            dos.writeBoolean(true);

            byte[] ip = ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress().getAddress();
            SeedRecord seedRecord = SeedRecord.builder().ip(ip).port(port).build();
            SEEDS.add(seedRecord);
        }

        private void handleSources(DataInputStream dis, DataOutputStream dos) throws IOException {
            // we dont care about input
            dis.readInt();

            if (oneSeed) {
                // 1 seed
                dos.writeInt(1);
                // first seed has file, second asking for it
                SeedRecord record = SEEDS.get(0);
                dos.write(record.getIp());
                dos.writeShort(record.getPort());
            } else {
                // 2 seed
                dos.writeInt(2);
                SeedRecord record = SEEDS.get(0);
                dos.write(record.getIp());
                dos.writeShort(record.getPort());
                record = SEEDS.get(1);
                dos.write(record.getIp());
                dos.writeShort(record.getPort());
            }
        }

        private void handleList(DataInputStream dis, DataOutputStream dos) throws IOException {
            // only 1 file
            dos.writeInt(1);
            dos.writeInt(freeId);
            dos.writeUTF(FILE_A1.getFileName().toString());
            dos.writeLong(Files.size(FILE_A1));
        }
    }

    private final class StubCallback implements FutureCallback<Object> {
        private boolean failed = false;

        @Override
        public void onSuccess(Object result) {
        }

        @Override
        public void onFailure(Throwable t) {
            failed = true;
        }

        boolean isFailed() {
            return failed;
        }
    }

    private class MockTracker implements Runnable {
        @NotNull
        private final ExecutorService handleService;
        @NotNull
        private final ServerSocket serverSocket;

        private boolean isRunning = true;

        MockTracker() throws IOException {
            serverSocket = new ServerSocket(TEST_PORT_TRACKER);
            handleService = Executors.newFixedThreadPool(3);
        }

        @Override
        public void run() {
            while (isRunning) {
                try {
                    final Socket clientSocket;
                    clientSocket = serverSocket.accept();
                    handleService.execute(new MockConnectionHandler(clientSocket));
                } catch (IOException ignored) {
                }
            }
        }

        void stop() throws IOException {
            isRunning = false;
            serverSocket.close();
            handleService.shutdownNow();
        }
    }

}