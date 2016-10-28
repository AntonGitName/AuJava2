package ru.mit.spbau.antonpp.vcs.core;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.mit.spbau.antonpp.vcs.core.exceptions.InitException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RepositoryInitTest {

    private static Path testDir;

    @BeforeClass
    public static void createWorkingDir() throws Exception {
        final Path dir = Utils.getCurrentDir();
        testDir = dir.resolve("build/test-working-dir");
    }


    @Before
    public void setUp() throws Exception {
        Files.createDirectories(testDir);
        System.setProperty("user.dir", testDir.toString());

    }

    public void changeCurrentDir(Path path) {
        System.setProperty("user.dir", path.toString());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
    }

    @Test
    public void testInit() throws Exception {
        assertNull(Utils.getRoot());
        Repository.init();
        assertEquals(Utils.getRoot(), testDir);
    }

    @Test(expected = InitException.class)
    public void testDoubleInit() throws Exception {
        Repository.init();

        final Path subFolder = testDir.resolve("whatever");
        Files.createDirectories(subFolder);

        changeCurrentDir(subFolder);

        Repository.init();
    }
}