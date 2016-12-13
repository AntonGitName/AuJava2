package ru.mit.spbau.antonpp.torrent.tracker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.SeedRecord;
import ru.mit.spbau.antonpp.torrent.commons.protocol.CommonRequestCode;
import ru.mit.spbau.antonpp.torrent.commons.protocol.TrackerRequestCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * @author Anton Mordberg
 * @since 13.12.16
 */
public class TorrentTrackerTest {

    static final short TEST_PORT_TRACKER = (short) 8081;
    private static final short TEST_PORT1 = (short) 31001;
    private static final short TEST_PORT2 = (short) 31002;
    private static final short TEST_PORT3 = (short) 31003;
    private static final Path TEST_DIR = Paths.get("build/test");
    private static final Path TEST_TRACKER_PATH = TEST_DIR.resolve("tracker");
    private static final String HOST = "localhost";

    private static final int THREADS = 4;

    private static final FileRecord RECORD1 = FileRecord.builder().name("1").size(1000).build();
    private static final FileRecord RECORD2 = FileRecord.builder().name("2").size(2000).build();
    private static final FileRecord RECORD3 = FileRecord.builder().name("1").size(3000).build();
    private static final List<FileRecord> RECORDS = Arrays.asList(RECORD1, RECORD2, RECORD3);
    private static final List<Short> PORTS = Arrays.asList(TEST_PORT1, TEST_PORT2, TEST_PORT3);
    private static final Random RND = new Random();
    private TorrentTracker tracker;

    @Before
    public void setUp() throws Exception {
        Util.deleteDir(TEST_DIR);
        tracker = TorrentTracker.create(TEST_TRACKER_PATH, TEST_PORT_TRACKER);
    }

    @After
    public void tearDown() throws Exception {
        if (tracker != null) {
            tracker.close();
        }
        Thread.sleep(1000);
    }

    @Test
    public void testCreateDir() throws IOException {
        tracker.close();
        assertTrue(Files.exists(TEST_TRACKER_PATH));
        tracker = null;
    }

    @Test
    public void testSavesState() throws IOException {
        tracker.close();
        assertTrue(Files.exists(TEST_TRACKER_PATH));
        tracker = null;
    }

    @Test
    public void testSourcesConcurrent() throws IOException {
        int nTests = 1024;
        List<Integer> ids = concurrentUpload(nTests);
        Supplier<List<Integer>> sup = () -> IntStream
                .range(0, RND.nextInt(nTests))
                .mapToObj(x -> ids.get(RND.nextInt(nTests)))
                .collect(Collectors.toList());
        Set<Integer> files1 = new HashSet<>(sup.get());
        Set<Integer> files2 = new HashSet<>(sup.get());
        Set<Integer> files3 = new HashSet<>(sup.get());

        final List<List<Integer>> intermediateUpdates = IntStream.range(0, nTests).mapToObj(x -> sup.get())
                .collect(Collectors.toList());

        ExecutorService service = Executors.newFixedThreadPool(THREADS);
        final boolean[] failed = {false};
        long acceptedUpdates = IntStream.range(0, nTests).mapToObj(x -> service.submit(() -> {
            try {
                return MockClient.sendUpdate(Util.getRandomElement(PORTS), intermediateUpdates.get(x));
            } catch (IOException e) {
                failed[0] = true;
            }
            return null;
        })).map(x -> {
            try {
                return x.get();
            } catch (InterruptedException | ExecutionException e) {
                failed[0] = true;
            }
            return false;
        }).filter(x -> x).count();
        assertFalse(failed[0]);
        assertEquals(nTests, acceptedUpdates);
        MockClient.sendUpdate(TEST_PORT1, new ArrayList<>(files1));
        MockClient.sendUpdate(TEST_PORT2, new ArrayList<>(files2));
        MockClient.sendUpdate(TEST_PORT3, new ArrayList<>(files3));

        for (int i = 0; i < nTests; ++i) {
            List<Short> seedRecords = MockClient.sendSources(i).stream().map(SeedRecord::getPort).collect(Collectors.toList());
            assertEquals(files1.contains(i), seedRecords.contains(TEST_PORT1));
            assertEquals(files2.contains(i), seedRecords.contains(TEST_PORT2));
            assertEquals(files3.contains(i), seedRecords.contains(TEST_PORT3));
        }
    }

    @Test
    public void testSources() throws IOException {
        int id1 = MockClient.sendUpload(RECORD1);
        int id2 = MockClient.sendUpload(RECORD2);
        int id3 = MockClient.sendUpload(RECORD3);

        MockClient.sendUpdate(TEST_PORT1, Arrays.asList(id1, id2));
        MockClient.sendUpdate(TEST_PORT2, Arrays.asList(id1, id2, id3));
        MockClient.sendUpdate(TEST_PORT3, Collections.singletonList(id3));

        List<Short> result;

        result = MockClient.sendSources(id1).stream().map(SeedRecord::getPort).collect(Collectors.toList());
        assertEquals(2, result.size());
        assertTrue(result.contains(TEST_PORT1));
        assertTrue(result.contains(TEST_PORT2));

        result = MockClient.sendSources(id2).stream().map(SeedRecord::getPort).collect(Collectors.toList());
        assertEquals(2, result.size());
        assertTrue(result.contains(TEST_PORT1));
        assertTrue(result.contains(TEST_PORT2));

        result = MockClient.sendSources(id3).stream().map(SeedRecord::getPort).collect(Collectors.toList());
        assertEquals(2, result.size());
        assertTrue(result.contains(TEST_PORT3));
        assertTrue(result.contains(TEST_PORT2));
    }

    @Test
    public void testList() throws IOException {
        int id1 = MockClient.sendUpload(RECORD1);
        int id2 = MockClient.sendUpload(RECORD2);
        int id3 = MockClient.sendUpload(RECORD3);

        Map<Integer, FileRecord> result = MockClient.sendList();

        assertEquals(RECORD1, result.get(id1));
        assertEquals(RECORD2, result.get(id2));
        assertEquals(RECORD3, result.get(id3));

    }

    private List<Integer> concurrentUpload(int nTests) {
        ExecutorService service = Executors.newFixedThreadPool(THREADS);
        final boolean[] failed = {false};
        List<Integer> result = IntStream.range(0, nTests).mapToObj(x -> service.submit(() -> {
            try {
                return MockClient.sendUpload(Util.getRandomElement(RECORDS));
            } catch (IOException e) {
                failed[0] = true;
            }
            return null;
        })).map(x -> {
            try {
                return x.get();
            } catch (InterruptedException | ExecutionException e) {
                failed[0] = true;
            }
            return -1;
        }).filter(x -> x != -1).collect(Collectors.toList());
        assertFalse(failed[0]);
        return result;
    }

    @Test
    public void testListConcurrent() throws IOException {
        int nTests = 1024;
        concurrentUpload(nTests);
        Map<Integer, FileRecord> result = MockClient.sendList();
        assertEquals(nTests, result.size());
        for (int i = 0; i < nTests; ++i) {
            assertTrue(result.containsKey(i));
        }
    }

    @Test
    public void testUpload() throws IOException {
        Set<Integer> ids = new HashSet<>();
        ids.add(MockClient.sendUpload(RECORD1));
        ids.add(MockClient.sendUpload(RECORD2));
        ids.add(MockClient.sendUpload(RECORD3));
        assertEquals(3, ids.size());
    }

    @Test
    public void testUploadConcurrent() throws IOException {
        int nTests = 1024;
        long uniqueIds = concurrentUpload(nTests).stream().distinct().count();
        assertEquals(nTests, uniqueIds);
    }

    private static class MockClient {

        private static Socket clientSocket;
        private static DataInputStream inputStream;
        private static DataOutputStream outputStream;

        private static void init() throws IOException {
            clientSocket = new Socket(HOST, TEST_PORT_TRACKER);
            inputStream = new DataInputStream(clientSocket.getInputStream());
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
        }

        private static void close() throws IOException {
            outputStream.writeByte(CommonRequestCode.RQ_DC);
            outputStream.close();
            inputStream.close();
            clientSocket.close();
            outputStream = null;
            inputStream = null;
            clientSocket = null;
        }


        public static boolean sendUpdate(int clientPort, List<Integer> files) throws IOException {
            init();

            outputStream.writeByte(TrackerRequestCode.RQ_UPDATE);
            outputStream.writeShort(clientPort);

            outputStream.writeInt(files.size());
            for (final int availableFile : files) {
                outputStream.writeInt(availableFile);
            }

            boolean result = inputStream.readBoolean();

            close();

            return result;
        }

        public static Map<Integer, FileRecord> sendList() throws IOException {
            init();

            outputStream.writeByte(TrackerRequestCode.RQ_LIST);
            Map<Integer, FileRecord> result = new HashMap<>();
            int numFiles = inputStream.readInt();
            for (int i = 0; i < numFiles; ++i) {
                int id = inputStream.readInt();
                result.put(id, FileRecord.builder()
                        .name(inputStream.readUTF())
                        .size(inputStream.readLong())
                        .build());
            }

            close();

            return result;
        }

        public static List<SeedRecord> sendSources(int id) throws IOException {
            init();

            outputStream.writeByte(TrackerRequestCode.RQ_SOURCES);
            outputStream.writeInt(id);
            List<SeedRecord> result = new ArrayList<>();
            int numFiles = inputStream.readInt();
            byte[] ip = new byte[4];
            for (int i = 0; i < numFiles; ++i) {
                for (int j = 0; j < 4; ++j) {
                    ip[j] = inputStream.readByte();
                }
                result.add(SeedRecord.builder().ip(ip).port(inputStream.readShort()).build());
            }
            close();

            return result;
        }

        public static int sendUpload(FileRecord fr) throws IOException {
            return sendUpload(fr.getName(), fr.getSize());
        }

        public static int sendUpload(String name, long size) throws IOException {
            init();

            outputStream.writeByte(TrackerRequestCode.RQ_UPLOAD);
            outputStream.writeUTF(name);
            outputStream.writeLong(size);
            int result = inputStream.readInt();

            close();

            return result;
        }

    }
}