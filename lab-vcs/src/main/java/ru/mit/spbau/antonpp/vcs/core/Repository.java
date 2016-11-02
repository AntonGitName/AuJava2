package ru.mit.spbau.antonpp.vcs.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.branch.BranchResolver;
import ru.mit.spbau.antonpp.vcs.core.exceptions.*;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;
import ru.mit.spbau.antonpp.vcs.core.log.RepositoryLog;
import ru.mit.spbau.antonpp.vcs.core.revision.Commit;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.revision.WorkingDir;
import ru.mit.spbau.antonpp.vcs.core.status.FileStatus;
import ru.mit.spbau.antonpp.vcs.core.status.RevisionDiff;
import ru.mit.spbau.antonpp.vcs.core.status.Status;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides API to work with vcs. It is also serializable in order to keep track of HEAD.
 *
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Repository implements FileSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

    private Path root;
    private String headHash;

    public Repository() {
    }

    /**
     * Creates an empty repository in specified directory.
     *
     * @param currentDir directory where new repository must be created.
     * @throws InitException if another repository is already in specified directory or if internal files cannot be
     *                       created due to some IO reasons.
     */
    public static void init(Path currentDir) throws InitException {
        final Path root = Utils.getRoot();
        if (root != null) {
            throw new InitException(String.format("This folder has already an initialised repository at %s.", root));
        } else {
            final Path internals = Utils.getInternals(currentDir);
            try {
                // create all required internal dirs
                Files.createDirectories(internals);
                Files.createDirectories(Utils.getStageFiles(currentDir));
                Files.createDirectories(Utils.getRevisionsDir(currentDir));

                // create Stage and repository
                final Stage stage = new Stage();
                stage.setRoot(currentDir);
                final String branch = "master";
                stage.setBranch(branch);
                final String initialCommitHash = stage.commit();
                final Repository repository = new Repository();
                repository.setRoot(currentDir);
                repository.setHeadHash(initialCommitHash);
                repository.serialize(Utils.getRepository(currentDir));

                // create all required internal files
                repository.saveStage(stage);
                final BranchResolver branchResolver = new BranchResolver();
                branchResolver.updateBranch(branch, initialCommitHash);
                repository.saveBranchResolver(branchResolver);
                repository.saveLog(new RepositoryLog());

                LOGGER.debug("Commit {} created in {}", initialCommitHash, currentDir);
            } catch (SerializationException | CommitException | IOException e) {
                throw new InitException(e);
            }
        }
    }

    // for tests
    @NotNull String getHeadHash() {
        return headHash;
    }

    private void setHeadHash(@NotNull String headHash) {
        this.headHash = headHash;
    }

    private void setRoot(@NotNull Path root) {
        this.root = root;
    }

    /**
     * Saves state of the repository to the disk.
     *
     * @param path a location where object can be serialized.
     * @throws SerializationException if could not write data to the filesystem.
     */
    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            Utils.serializePath(root, os);
            os.writeObject(headHash);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize repository", e);
        }
    }

    /**
     * Loads state of the repository from the disk.
     *
     * @param path a location where object was serialized.
     * @throws SerializationException if could not read data from the filesystem.
     */
    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            root = Utils.deserializePath(os);
            headHash = (String) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    /**
     * Loads {@code Stage} into the memory.
     *
     * @return deserialized {@code Stage}.
     * @throws SerializationException if could not read data from the filesystem.
     */
    Stage loadStage() throws SerializationException {
        final Stage stage = new Stage();
        stage.deserialize(Utils.getStageIndex(root));
        return stage;
    }

    /**
     * Saves {@code Stage} to the disk.
     *
     * @param stage instance to save
     * @throws SerializationException if could not write data to the filesystem.
     */
    private void saveStage(Stage stage) throws SerializationException {
        stage.serialize(Utils.getStageIndex(root));
    }

    /**
     * Loads {@code RepositoryLog} into the memory.
     *
     * @return deserialized {@code RepositoryLog}.
     * @throws SerializationException if could not read data from the filesystem.
     */
    private RepositoryLog loadLog() throws SerializationException {
        final RepositoryLog repositoryLog = new RepositoryLog();
        repositoryLog.deserialize(Utils.getLogFile(root));
        return repositoryLog;
    }

    /**
     * Saves {@code RepositoryLog} to the disk.
     *
     * @param repositoryLog instance to save
     * @throws SerializationException if could not write data to the filesystem.
     */
    private void saveLog(RepositoryLog repositoryLog) throws SerializationException {
        repositoryLog.serialize(Utils.getLogFile(root));
    }

    /**
     * Loads {@code Commit} that was marked as HEAD into the memory.
     *
     * @return deserialized {@code Commit}.
     * @throws SerializationException if could not read data from the filesystem.
     */
    private Commit loadHead() throws SerializationException {
        return loadCommit(headHash);
    }

    private Commit loadCommit(String hash) throws SerializationException {
        final Commit commit = new Commit();
        commit.deserialize(Utils.getRevisionIndex(root, hash));
        return commit;
    }

    /**
     * Creates a new commit from files that were added to the stage. Adds information about new commit to the getLogRecords.
     * Updates HEAD.
     *
     * @param info details that must be saved in getLogRecords file.
     * @throws SerializationException if could not write data to the filesystem.
     * @throws CommitException        if could not write data to the filesystem (wrapping exception to provide more details).
     */
    public void commit(CommitInfo info) throws SerializationException, CommitException {
        final Stage stage = loadStage();
        if (info.getMsg() == null) {
            info.setMsg(generateCommitMessage(stage));
        }
        final String commitHash = stage.commit();
        headHash = commitHash;
        if (stage.getBranch() != null) {
            final BranchResolver branchResolver = loadBranchResolver();
            branchResolver.updateBranch(stage.getBranch(), commitHash);
            saveBranchResolver(branchResolver);
        }
        info.setHash(commitHash);
        saveStage(stage);

        appendLogRecord(info);
    }

    /**
     * Merges specified commit with staged files. Updates HEAD and getLogRecords.
     *
     * @param revName branch name or hash prefix of the commit
     * @param info    information about merge that is added to the getLogRecords file.
     * @throws SerializationException if could not read/write data to the filesystem.
     * @throws MergeException         if specified commit was not found in repository
     */
    public void merge(String revName, CommitInfo info) throws SerializationException, MergeException, CheckoutException {
        final String commitHash = findCommitByHashOrBranch(revName);
        final Commit commit = loadCommit(commitHash);
        final Stage stage = loadStage();

        if (info.getMsg() == null) {
            info.setMsg(generateMergeMessage(headHash, commitHash));
        }

        final String mergeCommitHash = stage.merge(commit);
        setHeadHash(mergeCommitHash);
        if (stage.getBranch() != null) {
            final BranchResolver branchResolver = loadBranchResolver();
            branchResolver.updateBranch(stage.getBranch(), mergeCommitHash);
            saveBranchResolver(branchResolver);
        }
        saveStage(stage);
        info.setHash(mergeCommitHash);
        appendLogRecord(info);
    }

    private void appendLogRecord(CommitInfo info) throws SerializationException {
        final RepositoryLog repositoryLog = loadLog();
        repositoryLog.addRecord(info);
        saveLog(repositoryLog);
    }

    private String findCommitByHashOrBranch(String id) throws CheckoutException, SerializationException {
        final BranchResolver branchResolver = loadBranchResolver();
        if (branchResolver.hasBranch(id)) {
            id = branchResolver.getBranchHead(id);
        }
        final List<Path> paths;
        try {
            paths = Utils.findRevisionByHash(root, id);
        } catch (IOException e) {
            throw new CheckoutException("Could not read revisions", e);
        }
        if (paths.size() != 1) {
            throw new CheckoutException(String.format("Could not find single commit. Found: %s", paths));
        }
        return paths.get(0).getFileName().toString();
    }

    private String generateMergeMessage(String headHash, String commitHash) {
        try {
            final BranchResolver branchResolver = loadBranchResolver();
            final String headBranch = branchResolver.findCommitBranch(headHash);
            final String commitBranch = branchResolver.findCommitBranch(commitHash);
            final String nameHead = headBranch != null ? headBranch : headHash;
            final String nameCommit = commitBranch != null ? commitBranch : commitHash;
            return String.format("Merged %s with %s", nameHead, nameCommit);
        } catch (SerializationException e) {
            LOGGER.warn("failed to generate merge message");
            return null;
        }
    }

    private String generateCommitMessage(Stage stage) {
        try {
            final Commit head = loadHead();
            final RevisionDiff revisionDiff = new RevisionDiff(head, stage);
            final Set<Map.Entry<Path, FileStatus>> entries = revisionDiff.getFiles().entrySet();
            final long added = entries.stream().filter(x -> x.getValue() == FileStatus.ADDED).count();
            final long modified = entries.stream().filter(x -> x.getValue() == FileStatus.MODIFIED).count();
            final long removed = entries.stream().filter(x -> x.getValue() == FileStatus.REMOVED).count();
            return String.format("Added: %d, Modified: %d, Removed: %d", added, modified, removed);
        } catch (SerializationException e) {
            LOGGER.warn("Failed to generate commit message", e);
            return null;
        }
    }

    public void addChanges(Path path) throws SerializationException, StageAddException {
        final Stage stage = loadStage();
        stage.addChanges(path);
        saveStage(stage);
    }

    public String status() throws SerializationException {
        final Stage stage = loadStage();
        final Commit head = loadHead();
        final String status = new Status(head, stage).toString();
        saveStage(stage);
        return status;
    }

    public void reset(Path path) throws SerializationException, ResetException {
        final Stage stage = loadStage();
        stage.reset(path);
        saveStage(stage);
    }

    public List<CommitInfo> getLogRecords() throws SerializationException {
        final RepositoryLog repositoryLog = loadLog();
        return repositoryLog.getLog();
    }

    public void clean() throws SerializationException, IOException {
        final Stage stage = loadStage();
        final WorkingDir workingDir = new WorkingDir(root);
        final RevisionDiff diff = new RevisionDiff(stage, workingDir);

        final List<Path> untracked = diff.getFiles().entrySet().stream().filter(x -> x.getValue() == FileStatus.ADDED)
                .map(Map.Entry::getKey).collect(Collectors.toList());

        for (final Path path : untracked) {
            Files.delete(path);
            LOGGER.debug("Removed untracked file: {}", path);
        }
    }

    private BranchResolver loadBranchResolver() throws SerializationException {
        final BranchResolver branchResolver = new BranchResolver();
        branchResolver.deserialize(Utils.getBranchesFile(root));
        return branchResolver;
    }

    private void saveBranchResolver(BranchResolver branchResolver) throws SerializationException {
        branchResolver.serialize(Utils.getBranchesFile(root));
    }

    public void deleteBranch(String branch) throws SerializationException, BranchException {
        final Stage stage = loadStage();
        final BranchResolver branchResolver = loadBranchResolver();
        if (branchResolver.hasBranch(branch)) {
            branchResolver.deleteBranch(branch);
            if (branch.equals(stage.getBranch())) {
                stage.setBranch(null);
                saveStage(stage);
            }
            saveBranchResolver(branchResolver);
        } else {
            if (branch.equals(stage.getBranch())) {
                stage.setBranch(null);
                saveStage(stage);
            } else {
                throw new BranchException("No such branch");
            }
        }
    }

    public void addBranch(String branch) throws SerializationException, BranchException {
        final Stage stage = loadStage();
        final Commit head = loadHead();
        final BranchResolver branchResolver = loadBranchResolver();
        if (branchResolver.hasBranch(branch)) {
            throw new BranchException("Branch with this name already exists");
        }

        if (stage.getBranch() != null && !isStageClear(stage, head)) {
            throw new BranchException("You must commit changes before changing branch");
        }

        stage.setBranch(branch);
        saveStage(stage);
    }

    private boolean isStageClear(Stage stage, Commit head) {
        final RevisionDiff revisionDiff = new RevisionDiff(head, stage);
        final Set<Map.Entry<Path, FileStatus>> entries = revisionDiff.getFiles().entrySet();
        return entries.stream().allMatch(x -> x.getValue() == FileStatus.UNCHANGED);
    }

    public void checkout(String revName) throws CheckoutException, SerializationException {
        final Stage stage = loadStage();
        Commit head = loadHead();
        if (!isStageClear(stage, head)) {
            throw new CheckoutException("You must commit changes before checkout");
        }
        setHeadHash(findCommitByHashOrBranch(revName));
        head = loadHead();
        stage.checkoutRevision(head);
        stage.setBranch(getCommitBranch(revName));
        saveStage(stage);
    }

    @Nullable
    private String getCommitBranch(String hash) throws SerializationException {
        return loadBranchResolver().findCommitBranch(hash);
    }

    // for tests only
    Status getDetailedStatus() throws SerializationException {
        final Stage stage = loadStage();
        final Commit head = loadHead();
        return new Status(head, stage);
    }
}
