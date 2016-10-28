package ru.mit.spbau.antonpp.vcs.core;

import org.jetbrains.annotations.NotNull;
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
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Repository implements FileSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

    private Path root;
    private String headHash;

    public Repository() {
    }

    public static void init() throws InitException {
        final Path currentDir = Utils.getCurrentDir();
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

    public void setHeadHash(@NotNull String headHash) {
        this.headHash = headHash;
    }

    public void setRoot(@NotNull Path root) {
        this.root = root;
    }

    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
//            os.writeObject(root.toString());
            Utils.serializePath(root, os);
            os.writeObject(headHash);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize repository", e);
        }
    }

    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
//            root = Paths.get((String) os.readObject());
            root = Utils.deserializePath(os);
            headHash = (String) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    Stage loadStage() throws SerializationException {
        final Stage stage = new Stage();
        stage.deserialize(Utils.getStageIndex(root));
        return stage;
    }

    private void saveStage(Stage stage) throws SerializationException {
        stage.serialize(Utils.getStageIndex(root));
    }

    private RepositoryLog loadLog() throws SerializationException {
        final RepositoryLog repositoryLog = new RepositoryLog();
        repositoryLog.deserialize(Utils.getLogFile(root));
        return repositoryLog;
    }

    private void saveLog(RepositoryLog repositoryLog) throws SerializationException {
        repositoryLog.serialize(Utils.getLogFile(root));
    }

    private Commit loadHead() throws SerializationException {
        final Commit commit = new Commit();
        commit.deserialize(Utils.getRevisionIndex(root, headHash));
        return commit;
    }

    public void commit(CommitInfo info) throws SerializationException, CommitException {
        final RepositoryLog repositoryLog = loadLog();
        final Stage stage = loadStage();
        if (info.getMsg() == null) {
            info.setMsg(generateCommitMessage(stage));
        }
        final String hash = stage.commit();
        headHash = hash;
        if (stage.getBranch() != null) {
            final BranchResolver branchResolver = loadBranchResolver();
            branchResolver.updateBranch(stage.getBranch(), hash);
            saveBranchResolver(branchResolver);
        }
        info.setHash(hash);
        saveStage(stage);
        repositoryLog.addRecord(info);
        saveLog(repositoryLog);
    }

    public void merge(String revName, CommitInfo info) throws SerializationException, MergeException {
        final BranchResolver branchResolver = loadBranchResolver();
        if (branchResolver.hasBranch(revName)) {
            try {
                revName = branchResolver.getBranchHead(revName);
            } catch (BranchException e) {
                // impossible
            }
        }
        mergeByHash(revName, info);
    }

    private void mergeByHash(String commitHash, CommitInfo info) throws SerializationException, MergeException {
        final Commit commit = new Commit();
        try {
            commit.deserialize(Utils.getRevisionIndex(root, commitHash));
        } catch (SerializationException e) {
            throw new MergeException("No commit find with this hash", e);
        }
        final RepositoryLog repositoryLog = loadLog();
        final Stage stage = loadStage();
        if (info.getMsg() == null) {
            info.setMsg(generateMergeMessage(headHash, commitHash));
        }
        final String hash = stage.merge(commit);
        headHash = hash;
        if (stage.getBranch() != null) {
            final BranchResolver branchResolver = loadBranchResolver();
            branchResolver.updateBranch(stage.getBranch(), hash);
            saveBranchResolver(branchResolver);
        }
        saveStage(stage);
        info.setHash(hash);
        repositoryLog.addRecord(info);
        saveLog(repositoryLog);
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

    public List<CommitInfo> log() throws SerializationException {
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

    private void checkoutHash(String hashPrefix, String branch) throws CheckoutException, SerializationException {
        final List<Path> paths;
        try {
            paths = Utils.findRevisionByHash(root, hashPrefix);
        } catch (IOException e) {
            throw new CheckoutException("Could not read revisions", e);
        }
        if (paths.size() != 1) {
            throw new CheckoutException(String.format("Could not find single commit. Found: %s", paths));
        }
        final Stage stage = loadStage();
        Commit head = loadHead();
        if (!isStageClear(stage, head)) {
            throw new CheckoutException("You must commit changes before checkout");
        }

        headHash = paths.get(0).getFileName().toString();
        head = loadHead();
        stage.checkoutRevision(head);
        stage.setBranch(branch);
        saveStage(stage);
    }

    public void checkout(String revName) throws CheckoutException, SerializationException {
        final BranchResolver branchResolver = loadBranchResolver();
        final String branch;
        if (branchResolver.hasBranch(revName)) {
            try {
                revName = branchResolver.getBranchHead(revName);
            } catch (BranchException e) {
                // impossible
            }
            branch = revName;
        } else {
            branch = branchResolver.findCommitBranch(revName);
        }
        checkoutHash(revName, branch);
    }

    // for tests only
    Status getDetailedStatus() throws SerializationException {
        final Stage stage = loadStage();
        final Commit head = loadHead();
        return new Status(head, stage);
    }
}
