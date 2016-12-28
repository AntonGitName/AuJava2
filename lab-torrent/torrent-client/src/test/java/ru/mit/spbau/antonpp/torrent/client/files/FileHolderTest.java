package ru.mit.spbau.antonpp.torrent.client.files;

import com.google.common.io.Files;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author antonpp
 * @since 25/12/2016
 */
public class FileHolderTest {

    private static final Random RND = new Random();
    private static final long HUGE_FILE_SIZE = 512L * 1024 * 1024; // 512 MB

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Ignore("this test takes a while to run, do not start it if you want only fresh build")
    @Test
    public void testCreateHugeFile() throws Exception {
        final String name = "huge_file";
        final Path testDir = Paths.get(folder.getRoot().getCanonicalPath());
        final Path cmpFileLocation = testDir.resolve("result");

        final Path hugeFilePath = testDir.resolve(name);
        folder.newFile(name);

        int chunkLength = 1024 * 8;
        byte[] chunk = new byte[chunkLength];
        try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(hugeFilePath.toFile()))) {
            for (long i = 0; i * chunkLength < HUGE_FILE_SIZE; ++i) {
                RND.nextBytes(chunk);
                fos.write(chunk);
            }
        }
        final TrackerFileRecord record = TrackerFileRecord.builder().id(1).size(HUGE_FILE_SIZE).name(name).build();

        ScheduledExecutorService memoryChecker = Executors.newSingleThreadScheduledExecutor();
        final boolean[] memoryOverused = {false};
        memoryChecker.scheduleAtFixedRate(() -> {
            final long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            memoryOverused[0] = usedMemory > HUGE_FILE_SIZE / 2;

        }, 0, 1, TimeUnit.SECONDS);
        try {
            final FileHolder fileHolder = FileHolder.create(hugeFilePath, testDir, record);
            fileHolder.copyFile(cmpFileLocation);
            assertTrue(Files.equal(hugeFilePath.toFile(), cmpFileLocation.toFile()));
        } finally {
            memoryChecker.shutdownNow();
            assertFalse(memoryOverused[0]);
        }

    }

}