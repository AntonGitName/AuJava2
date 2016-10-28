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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Repository implements FileSerializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Repository.class);

    @NotNull
    private Path root;

    @NotNull
    private String headHash;

    public Repository() {
    }

    public void setRoot(@NotNull Path root) {
        this.root = root;
    }

    public void setHeadHash(@NotNull String headHash) {
        this.headHash = headHash;
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

    private Stage loadStage() throws SerializationException {
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
            info.setMsg(generateMessage(stage));
        }
        final String hash = stage.commit();
        headHash = hash;
        if (stage.getBranch() != null) {
            final BranchResolver branchResolver = loadBranchResolver();
            branchResolver.updateBranch(stage.getBranch(), hash);
            saveBranchResolver(branchResolver);
        }
        info.setHash(hash);
        stage.setParents(Collections.singletonList(hash));
        saveStage(stage);
        repositoryLog.addRecord(info);
        saveLog(repositoryLog);
    }

    private String generateMessage(Stage stage) {
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

    public List<CommitInfo> log() throws SerializationException, CommitException {
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
        branchResolver.deserialize(Utils.getStageIndex(root));
        return branchResolver;
    }

    private void saveBranchResolver(BranchResolver branchResolver) throws SerializationException {
        branchResolver.serialize(Utils.getLogFile(root));
    }

    public void deleteBranch(String branch) throws SerializationException, BranchException {
        final Stage stage = loadStage();
        final BranchResolver branchResolver = loadBranchResolver();
        branchResolver.deleteBranch(branch);
        if (branch.equals(stage.getBranch())) {
            stage.setBranch(branch);
            saveStage(stage);
        }
        saveBranchResolver(branchResolver);
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
            final Optional<String> optional = branchResolver.findCommitBranch(revName);
            if (optional.isPresent()) {
                branch = optional.get();
            } else {
                branch = null;
            }
        }
        checkoutHash(revName, branch);
    }
}
