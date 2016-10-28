package ru.mit.spbau.antonpp.vcs.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;
import ru.mit.spbau.antonpp.vcs.core.status.FileStatus;
import ru.mit.spbau.antonpp.vcs.core.status.Status;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static ru.mit.spbau.antonpp.vcs.core.status.FileStatus.*;

public class RepositoryTest {

    private static Path testDir;
    private Repository repository;

    @BeforeClass
    public static void createWorkingDir() throws Exception {
        final Path dir = Utils.getCurrentDir();
        testDir = dir.resolve("build/test-working-dir");
        FileUtils.deleteDirectory(testDir.toFile());
    }

    private Repository loadRepository() throws SerializationException {
        Repository r = new Repository();
        r.deserialize(Utils.getRepository(Utils.getRoot()));
        return r;
    }

    @Before
    public void setUp() throws Exception {
        Files.createDirectories(testDir);
        System.setProperty("user.dir", testDir.toString());
        Repository.init();
        repository = loadRepository();
    }

    public void createFile(String content, Path path) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            IOUtils.write(content, fos, Charset.defaultCharset());
        }
    }

    public void changeCurrentDir(Path path) {
        System.setProperty("user.dir", path.toString());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
    }

    @Test
    public void testCommit() throws Exception {

    }

    @Test
    public void testAddChanges() throws Exception {
        final Path newFile = testDir.resolve("123.txt");
        createFile("123", newFile);
        repository.addChanges(newFile);

        final Status detailedStatus = repository.getDetailedStatus();
        assertEquals(detailedStatus.getHeadDiff().getFiles().get(newFile), FileStatus.ADDED);
    }

    @Test
    public void testAddChangesRemoveFile() throws Exception {
        final Path newFile = testDir.resolve("123.txt");
        createFile("123", newFile);
        repository.addChanges(newFile);
        repository.commit(new CommitInfo());
        Files.delete(newFile);

        repository.addChanges(newFile);

        final Status detailedStatus = repository.getDetailedStatus();
        assertEquals(detailedStatus.getHeadDiff().getFiles().get(newFile), REMOVED);
    }

    @Test
    public void testAddChangesModifyFile() throws Exception {
        final Path newFile = testDir.resolve("123.txt");
        createFile("123", newFile);
        repository.addChanges(newFile);
        repository.commit(new CommitInfo());
        createFile("456", newFile);

        repository.addChanges(newFile);

        final Status detailedStatus = repository.getDetailedStatus();
        assertEquals(detailedStatus.getHeadDiff().getFiles().get(newFile), MODIFIED);
    }

    @Test
    public void testAddChangesUnchandedFile() throws Exception {
        final Path newFile = testDir.resolve("123.txt");
        createFile("123", newFile);
        repository.addChanges(newFile);
        repository.commit(new CommitInfo());
        createFile("123", newFile);

        repository.addChanges(newFile);

        final Status detailedStatus = repository.getDetailedStatus();
        assertEquals(detailedStatus.getHeadDiff().getFiles().get(newFile), UNCHANGED);
    }

    @Test
    public void testStatus() throws Exception {
        final Path stageUnchangedWorkUnchanged = testDir.resolve("stageUnchangedWorkUnchanged.txt");
        final Path stageUnchangedWorkRemoved = testDir.resolve("stageUnchangedWorkRemoved.txt");
        final Path stageUnchangedWorkModified = testDir.resolve("stageUnchangedWorkModified.txt");

        final Path stageAddedWorkUnchanged = testDir.resolve("stageAddedWorkUnchanged.txt");
        final Path stageAddedWorkModified = testDir.resolve("stageAddedWorkModified.txt");
        final Path stageAddedWorkRemoved = testDir.resolve("stageAddedWorkRemoved.txt");

        final Path stageModifiedWorkUnchanged = testDir.resolve("stageModifiedWorkUnchanged.txt");
        final Path stageModifiedWorkModified = testDir.resolve("stageModifiedWorkModified.txt");
        final Path stageModifiedWorkRemoved = testDir.resolve("stageModifiedWorkRemoved.txt");

        final Path stageRemovedWorkAdded = testDir.resolve("stageRemovedWorkAdded.txt");
        final Path untracked = testDir.resolve("untracked.txt");


        createFile("stageUnchangedWorkUnchanged", stageUnchangedWorkUnchanged);
        repository.addChanges(stageUnchangedWorkUnchanged);
        createFile("stageUnchangedWorkRemoved", stageUnchangedWorkRemoved);
        repository.addChanges(stageUnchangedWorkRemoved);
        createFile("stageUnchangedWorkModified", stageUnchangedWorkModified);
        repository.addChanges(stageUnchangedWorkModified);

        createFile("stageModifiedWorkUnchanged", stageModifiedWorkUnchanged);
        repository.addChanges(stageModifiedWorkUnchanged);
        createFile("stageModifiedWorkModified", stageModifiedWorkModified);
        repository.addChanges(stageModifiedWorkModified);
        createFile("stageModifiedWorkRemoved", stageModifiedWorkRemoved);
        repository.addChanges(stageModifiedWorkRemoved);

        createFile("stageRemovedWorkAdded", stageRemovedWorkAdded);
        repository.addChanges(stageRemovedWorkAdded);

        createFile("untracked", untracked);

        repository.commit(new CommitInfo());

        Files.delete(stageUnchangedWorkRemoved);
        createFile("stageUnchangedWorkModifiedM", stageUnchangedWorkModified);

        createFile("stageModifiedWorkUnchangedM", stageModifiedWorkUnchanged);
        repository.addChanges(stageModifiedWorkUnchanged);

        createFile("stageUnchangedWorkModifiedM", stageUnchangedWorkModified);
        createFile("stageModifiedWorkModifiedM", stageModifiedWorkModified);
        repository.addChanges(stageModifiedWorkModified);
        createFile("stageModifiedWorkModifiedMM", stageModifiedWorkModified);

        createFile("stageModifiedWorkRemovedM", stageModifiedWorkRemoved);
        repository.addChanges(stageModifiedWorkRemoved);
        Files.delete(stageModifiedWorkRemoved);

        createFile("stageAddedWorkUnchanged", stageAddedWorkUnchanged);
        repository.addChanges(stageAddedWorkUnchanged);

        createFile("stageAddedWorkModified", stageAddedWorkModified);
        repository.addChanges(stageAddedWorkModified);
        createFile("stageAddedWorkModifiedM", stageAddedWorkModified);

        createFile("stageAddedWorkRemoved", stageAddedWorkRemoved);
        repository.addChanges(stageAddedWorkRemoved);
        Files.delete(stageAddedWorkRemoved);

        Files.delete(stageRemovedWorkAdded);
        repository.addChanges(stageRemovedWorkAdded);
        createFile("stageRemovedWorkAdded", stageRemovedWorkAdded);

        final Status detailedStatus = repository.getDetailedStatus();
        final Map<Path, FileStatus> headDiff = detailedStatus.getHeadDiff().getFiles();
        final Map<Path, FileStatus> stageDiff = detailedStatus.getStageDiff().getFiles();

        assertFileStatus(headDiff, stageDiff, UNCHANGED, UNCHANGED, stageUnchangedWorkUnchanged);
        assertFileStatus(headDiff, stageDiff, UNCHANGED, REMOVED, stageUnchangedWorkRemoved);
        assertFileStatus(headDiff, stageDiff, UNCHANGED, MODIFIED, stageUnchangedWorkModified);
        assertFileStatus(headDiff, stageDiff, ADDED, UNCHANGED, stageAddedWorkUnchanged);
        assertFileStatus(headDiff, stageDiff, ADDED, MODIFIED, stageAddedWorkModified);
        assertFileStatus(headDiff, stageDiff, ADDED, REMOVED, stageAddedWorkRemoved);
        assertFileStatus(headDiff, stageDiff, MODIFIED, UNCHANGED, stageModifiedWorkUnchanged);
        assertFileStatus(headDiff, stageDiff, MODIFIED, MODIFIED, stageModifiedWorkModified);
        assertFileStatus(headDiff, stageDiff, MODIFIED, REMOVED, stageModifiedWorkRemoved);
        assertFileStatus(headDiff, stageDiff, REMOVED, ADDED, stageRemovedWorkAdded);
        assertEquals(ADDED, stageDiff.get(untracked));

        System.out.println(repository.status());
    }

    private void assertFileStatus(Map<Path, FileStatus> headDiff, Map<Path, FileStatus> stageDiff, FileStatus head,
                                  FileStatus stage, Path path) {
        assertEquals(head, headDiff.get(path));
        assertEquals(stage, stageDiff.get(path));
    }

    @Test
    public void testReset() throws Exception {

    }

    @Test
    public void testLog() throws Exception {

    }

    @Test
    public void testClean() throws Exception {

    }

    @Test
    public void testDeleteBranch() throws Exception {

    }

    @Test
    public void testAddBranch() throws Exception {

    }

    @Test
    public void testCheckout() throws Exception {

    }
}