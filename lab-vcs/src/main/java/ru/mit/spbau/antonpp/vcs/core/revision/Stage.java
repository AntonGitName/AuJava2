package ru.mit.spbau.antonpp.vcs.core.revision;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.*;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Stage extends AbstractRevision {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stage.class);

    @Nullable
    private String branch;

    @Nullable
    public String getBranch() {
        return branch;
    }

    public void setBranch(@Nullable String branch) {
        this.branch = branch;
    }

    public void reset(Path path) throws ResetException {
        final Commit parentWithFile;
        try {
            parentWithFile = findParentWithFile(path);
        } catch (SerializationException e) {
            throw new ResetException("Could not access parent revision", e);
        }
        if (parentWithFile == null) {
            LOGGER.debug("Reseting not versioned file");
            if (!checkFile(path)) {
                throw new ResetException();
            }
            try {
                Files.delete(getRealFileLocation(path));
            } catch (IOException e) {
                throw new ResetException("Could not remove file from stage", e);
            }
            index.remove(path);
        } else {
            LOGGER.debug("Reseting versioned file");
            final String fileHash = parentWithFile.getFileHash(path);
            final Path stageLocation = Utils.getStageFiles(root).resolve(fileHash);
            try {
                Files.copy(parentWithFile.getRealFileLocation(path), stageLocation, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(parentWithFile.getRealFileLocation(path), path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ResetException("Could not copy file from parent revision", e);
            }
            index.put(path, stageLocation);
        }
    }

    public void addChanges(Path path) throws StageAddException {
        try {
            removeIfExist(path);
            if (Files.exists(path)) {
                // add or modify
                final String fileHash = Utils.getFileHash(root, path);
                final Path realPath = Utils.getStageFiles(root).resolve(fileHash);
                Files.copy(path, realPath, StandardCopyOption.REPLACE_EXISTING);
                index.put(path, realPath);
            } else {
                // remove
                index.remove(path);
            }
        } catch (IOException e) {
            throw new StageAddException(e);
        }

    }

    private void removeIfExist(Path path) throws IOException {
        if (index.containsKey(path)) {
            Files.delete(getRealFileLocation(path));
        }
    }

    public String commit() throws CommitException {
        final String commitHash = getRevHash();

        try {
            Files.createDirectories(Utils.getRevisionFiles(root, commitHash));
        } catch (IOException e) {
            throw new CommitException("Failed to create dir for the revision", e);
        }

        final Set<Path> files = listFiles();
        final Commit commit = new Commit();
        commit.setRoot(root);
        commit.setParents(getParents());
        for (final Path path : files) {
            final Commit parentWithThisFile;
            try {
                parentWithThisFile = findParentWithExactFile(path);
            } catch (SerializationException e) {
                throw new CommitException("Failed to deserialize parent revision", e);
            }
            if (parentWithThisFile != null) {
                commit.index.put(path, parentWithThisFile.getRealFileLocation(path));
            } else {
                try {
                    Utils.copyToDir(getRealFileLocation(path), Utils.getRevisionFiles(root, commitHash));
                    commit.index.put(path, Utils.getRevisionFiles(root, commitHash).resolve(getFileHash(path)));
                } catch (IOException e) {
                    throw new CommitException("Failed to move file", e);
                }
            }
        }
        try {
            commit.serialize(Utils.getRevisionIndex(root, commitHash));
        } catch (SerializationException e) {
            throw new CommitException("Failed to save commit", e);
        }
        setParents(Collections.singleton(commit.getRevHash()));
        return getRevHash();
    }

    private List<Commit> loadParents() throws SerializationException {
        final List<Commit> revisions = new ArrayList<>(getParents().size());
        for (final String parentHash : getParents()) {
            final Commit revision = new Commit();
            revision.deserialize(Utils.getRevisionIndex(root, parentHash));
            revisions.add(revision);
        }
        return revisions;
    }

    @Nullable
    private Commit findParentWithFile(Path path) throws SerializationException {
        final List<Commit> revisions = loadParents();
        Commit parentWithThisFile = null;
        for (final Commit revision : revisions) {
            if (revision.checkFile(path)) {
                parentWithThisFile = revision;
                break;
            }
        }
        return parentWithThisFile;
    }

    @Nullable
    private Commit findParentWithExactFile(Path path) throws SerializationException {
        final List<Commit> revisions = loadParents();
        Commit parentWithThisFile = null;
        final String fileHash = getFileHash(path);
        for (final Commit revision : revisions) {
            if (revision.checkFile(path, fileHash)) {
                parentWithThisFile = revision;
                break;
            }
        }
        return parentWithThisFile;
    }

    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            Utils.serializePath(root, os);
            os.writeObject(parents);
            os.writeObject(branch);
//            os.writeObject(index);
            Utils.serializeMapWithPath(index, os, Path.class, Path.class);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize revision", e);
        }
    }

    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            root = Utils.deserializePath(os);
            parents = (Set<String>) os.readObject();
            branch = (String) os.readObject();
//            index = (Map<Path, Path>) os.readObject();
            index = Utils.deserializeMapWithPath(os, Path.class, Path.class);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }


    public void checkoutRevision(AbstractRevision revision) throws CheckoutException {
        try {
            for (final Path path : listFiles()) {
                removeIfExist(path);
            }
            index.clear();
            for (final Path path : revision.listFiles()) {
                final Path dir = Utils.getStageFiles(root);
                final Path location = revision.getRealFileLocation(path);
                Utils.copyToDir(location, dir);
                Files.copy(location, path, StandardCopyOption.REPLACE_EXISTING);
                index.put(path, dir.resolve(location.getFileName()));
            }
            setParents(Collections.singleton(revision.getRevHash()));
            branch = null;
        } catch (IOException e) {
            throw new CheckoutException("Could not copy files", e);
        }
    }

    // accept ours always
    public String merge(Commit commitToMergeWith) throws MergeException {
        final Set<Path> filesToMergeIn = commitToMergeWith.listFiles();
        try {
            for (final Path path : filesToMergeIn) {
                if (!checkFile(path)) {
                    final Path dir = Utils.getStageFiles(root);
                    final Path location = commitToMergeWith.getRealFileLocation(path);
                    Utils.copyToDir(location, dir);
                    Files.copy(location, path, StandardCopyOption.REPLACE_EXISTING);
                    index.put(path, dir.resolve(location.getFileName()));
                }
            }
        } catch (IOException e) {
            throw new MergeException("Failed to copy new files", e);
        }

        setParents(Stream.concat(getParents().stream(), commitToMergeWith.getParents().stream())
                .collect(Collectors.toSet()));

        try {
            return commit();
        } catch (CommitException e) {
            throw new MergeException("Failed to commit merge", e);
        }
    }
}
