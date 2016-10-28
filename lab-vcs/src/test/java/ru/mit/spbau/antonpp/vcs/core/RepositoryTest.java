package ru.mit.spbau.antonpp.vcs.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.mit.spbau.antonpp.vcs.core.exceptions.BranchException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CheckoutException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.ResetException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.status.FileStatus;
import ru.mit.spbau.antonpp.vcs.core.status.Status;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.*;
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

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(testDir.toFile());
    }

    @Test
    public void testCommit() throws Exception {
        final Path added = testDir.resolve("added.txt");
        final Path unchanged = testDir.resolve("untracked.txt");
        final Path untracked = testDir.resolve("untracked.txt");
        final Path modified = testDir.resolve("modified.txt");
        final Path removed = testDir.resolve("removed.txt");

        createFile("unchanged", unchanged);
        createFile("untracked", untracked);
        createFile("modified", modified);
        createFile("removed", removed);

        repository.addChanges(unchanged);
        repository.addChanges(modified);
        repository.addChanges(removed);

        repository.commit(new CommitInfo());

        Files.delete(removed);
        createFile("modified_modified", modified);
        createFile("added", added);

        repository.addChanges(added);
        repository.addChanges(unchanged);
        repository.addChanges(modified);
        repository.addChanges(removed);


        final String initialHash = repository.getHeadHash();
        final int logLen = repository.log().size();

        final Map<Path, FileStatus> diffBefore = repository.getDetailedStatus().getHeadDiff().getFiles();
        assertTrue(diffBefore.entrySet().stream().anyMatch(x -> x.getValue() == ADDED));
        assertTrue(diffBefore.entrySet().stream().anyMatch(x -> x.getValue() == MODIFIED));
        assertTrue(diffBefore.entrySet().stream().anyMatch(x -> x.getValue() == UNCHANGED));

        repository.commit(new CommitInfo());

        assertNotEquals(initialHash, repository.getHeadHash());
        assertEquals(logLen + 1, repository.log().size());

        final Map<Path, FileStatus> diffAfter = repository.getDetailedStatus().getHeadDiff().getFiles();
        assertTrue(diffAfter.entrySet().stream().allMatch(x -> x.getValue() == UNCHANGED));
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
    public void testResetVersionedAndStaged() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.reset(versioned);
        assertEquals("versioned", Utils.getFileContent(versioned));
    }

    @Test(expected = ResetException.class)
    public void testResetNoVersion() throws Exception {
        final Path untracked = testDir.resolve("untracked.txt");
        createFile("untracked", untracked);
        repository.reset(untracked);
    }

    @Test
    public void testResetVersioned() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        createFile("versioned_modified", versioned);
        repository.reset(versioned);
        assertEquals("versioned", Utils.getFileContent(versioned));
    }

    @Test
    public void testResetStaged() throws Exception {
        final Path staged = testDir.resolve("staged.txt");
        createFile("staged", staged);
        repository.addChanges(staged);
        repository.reset(staged);
        assertEquals("staged", Utils.getFileContent(staged));
        final Status detailedStatus = repository.getDetailedStatus();
        assertEquals(detailedStatus.getStageDiff().getFiles().get(staged), ADDED);
    }

    @Test
    public void testMerge() throws Exception {
        final Path file1 = testDir.resolve("1.txt");
        final Path file2 = testDir.resolve("2.txt");
        final String branch1 = "branch1";
        final String branch2 = "branch2";
        repository.addBranch(branch1);
        createFile("1", file1);
        repository.addChanges(file1);
        repository.commit(new CommitInfo());
        repository.checkout("master");
        repository.addBranch(branch2);
        createFile("2", file2);
        repository.addChanges(file2);
        repository.commit(new CommitInfo());
        repository.merge(branch1, new CommitInfo());
        assertEquals("1", Utils.getFileContent(file1));
        assertEquals("2", Utils.getFileContent(file2));
    }

    @Test
    public void testLog() throws Exception {
        final Path newFile = testDir.resolve("123.txt");
        createFile("123", newFile);
        repository.commit(new CommitInfo());
        Files.delete(newFile);
        repository.commit(new CommitInfo());
        assertEquals(2, repository.log().size());

    }

    @Test(expected = BranchException.class)
    public void testDeleteNotExistingBranch() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String branch = "development";
        repository.deleteBranch(branch);
    }

    @Test
    public void testDeleteOldBranch() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String branch = "development";
        repository.addBranch(branch);
        assertEquals(branch, repository.loadStage().getBranch());

        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        assertEquals(branch, repository.loadStage().getBranch());

        repository.deleteBranch(branch);
        assertNull(repository.loadStage().getBranch());

        createFile("versioned_modified2", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        try {
            repository.checkout(branch);
            fail("Checkout on deleted branch");
        } catch (CheckoutException e) {
            // unused
        }
    }


    @Test
    public void testClean() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        final Path staged = testDir.resolve("staged.txt");
        final Path untracked = testDir.resolve("untracked.txt");
        createFile("versioned", versioned);
        createFile("staged", staged);
        createFile("untracked", untracked);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        repository.addChanges(staged);
        repository.clean();
        assertEquals("versioned", Utils.getFileContent(versioned));
        assertEquals("staged", Utils.getFileContent(staged));
        assertTrue(!Files.exists(untracked));
    }

    @Test
    public void testDeleteBranchStageOnly() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String branch = "development";
        repository.addBranch(branch);
        Stage stage = repository.loadStage();
        assertEquals(branch, stage.getBranch());
        repository.deleteBranch(branch);
        stage = repository.loadStage();
        assertNull(stage.getBranch());

        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        try {
            repository.checkout(branch);
            fail("Checkout on deleted branch");
        } catch (CheckoutException e) {
            // unused
        }
    }

    @Test(expected = BranchException.class)
    public void testAddBranchNotCommitedChanges() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        final String branch = "development";
        repository.addBranch(branch);
    }

    @Test
    public void testAddBranch() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String branch = "development";
        repository.addBranch(branch);
        final Stage stage = repository.loadStage();
        assertEquals(branch, stage.getBranch());
    }

    @Test
    public void testCheckoutByFullHash() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String headHash = repository.getHeadHash();
        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        repository.checkout(headHash);
        assertEquals(headHash, repository.getHeadHash());
        assertEquals("versioned", Utils.getFileContent(versioned));
    }

    @Test
    public void testCheckoutByShortHash() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String headHash = repository.getHeadHash();
        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        repository.checkout(headHash.substring(0, 6));
        assertEquals(headHash, repository.getHeadHash());
        assertEquals("versioned", Utils.getFileContent(versioned));

    }

    @Test(expected = CheckoutException.class)
    public void testCheckoutByAmbiguousHash() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        repository.checkout("");
    }

    @Test(expected = CheckoutException.class)
    public void testCheckoutByWrongHash() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String headHash = repository.getHeadHash();
        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        repository.checkout("BAD" + headHash.substring(0, 6));
    }

    @Test
    public void testCheckoutByBranch() throws Exception {
        final Path versioned = testDir.resolve("versioned.txt");
        createFile("versioned", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String headHash = repository.getHeadHash();
        final String oldBranch = "master";
        final String newBranch = "development";
        repository.addBranch(newBranch);
        createFile("versioned_modified", versioned);
        repository.addChanges(versioned);
        repository.commit(new CommitInfo());
        final String newBranchHash = repository.getHeadHash();
        repository.checkout(oldBranch);
        assertEquals(headHash, repository.getHeadHash());
        assertEquals("versioned", Utils.getFileContent(versioned));
        repository.checkout(newBranch);
        assertEquals(newBranchHash, repository.getHeadHash());
        assertEquals("versioned_modified", Utils.getFileContent(versioned));
    }
}