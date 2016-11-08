package ru.mit.spbau.antonpp.vcs.core.revision;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.Nullable;
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
 * Core class of the application. All changes to revisions are made here.
 *
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Slf4j
public final class Stage extends AbstractCommit {

    @Nullable
    private String revHash;

    @Getter @Setter
    @Nullable private String branch;

    /**
     * Resets file changes. If file is revisioned in HEAD, then it resets to this version. If it is only stage, then
     * file is removed from stage.
     *
     * @param path path to file.
     * @throws ResetException if revisions could not be loaded or in you case you are trying to reset unrevisioned file.
     */
    public void reset(Path path) throws ResetException {
        revHash = null;
        final Commit parentWithFile;
        try {
            parentWithFile = findParentWithFile(path);
        } catch (SerializationException e) {
            throw new ResetException("Could not access parent revision", e);
        }
        if (parentWithFile == null) {
            log.debug("Reseting not versioned file");
            if (!checkFileInRevision(path)) {
                throw new ResetException();
            }
            try {
                Files.delete(getRealFileLocation(path));
            } catch (IOException e) {
                throw new ResetException("Could not remove file from stage", e);
            }
            index.remove(path);
        } else {
            log.debug("Resetting versioned file");
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

    /**
     * Applies modification of the file to the stage. It can be one of three: file addition, file modification,
     * file removal.
     *
     * @param path path to file.
     * @throws StageAddException wrapped IO exception.
     */
    public void addChanges(Path path) throws StageAddException {
        revHash = null;
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

    /**
     * Safely removes file from stage index.
     *
     * @param path path to file.
     * @throws IOException if could not remove file.
     */
    private void removeIfExist(Path path) throws IOException {
        if (index.containsKey(path)) {
            Files.delete(getRealFileLocation(path));
        }
    }

    /**
     * Creates a new commit from files that were added to the stage. Stage itself has a copy of all revisioned files,
     * but a new commit will have copies of only that files that cannot be found in its parents indexes. That means if
     * this commit has 10 files in revision and only 2 of them are not the same as they very in parents revisions (i.e.
     * files were dded or modified) then only these 2 files will be stored on the disk with new commit, thus optimising
     * disk usage.
     *
     * @throws CommitException if could not write data to the filesystem (wrapping exception to provide more details).
     */
    public String commit() throws CommitException {
        val filesHash = getRevHash();
        val commit = new Commit();
        commit.generateSeed();
        commit.setFilesHash(filesHash);
        commit.setRoot(root);
        commit.setParents(getParents());
        val commitHash = commit.getRevHash();

        if (Files.exists(Utils.getRevisionDir(root, commitHash))) {
            throw new CommitException("Could not perform commit: revision with the same hash already exists");
        }

        try {
            Files.createDirectories(Utils.getRevisionFiles(root, commitHash));
        } catch (IOException e) {
            throw new CommitException("Failed to create dir for the revision", e);
        }

        final Set<Path> files = listFiles();

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
        setParents(Collections.singleton(commitHash));
        return commitHash;
    }

    /**
     * Loads from disk parent commits.
     *
     * @return List of parent commits.
     * @throws SerializationException if could not load internal files.
     */
    private List<Commit> loadParents() throws SerializationException {
        final List<Commit> revisions = new ArrayList<>(getParents().size());
        for (final String parentHash : getParents()) {
            final Commit revision = new Commit();
            revision.deserialize(Utils.getRevisionIndex(root, parentHash));
            revisions.add(revision);
        }
        return revisions;
    }

    /**
     * Finds one parent that has file with the same path as the specified file in its index.
     *
     * @param path path to file that is searched in indexes.
     * @return parent commit or null if none match.
     * @throws SerializationException if could not load internal files.
     */
    @Nullable
    private Commit findParentWithFile(Path path) throws SerializationException {
        final List<Commit> revisions = loadParents();
        Commit parentWithThisFile = null;
        for (final Commit revision : revisions) {
            if (revision.checkFileInRevision(path)) {
                parentWithThisFile = revision;
                break;
            }
        }
        return parentWithThisFile;
    }

    /**
     * Finds one parent that has the same version of the specified file in its index.
     *
     * @param path path to file that is searched in indexes.
     * @return parent commit or null if none match.
     * @throws SerializationException if could not load internal files.
     */
    @Nullable
    private Commit findParentWithExactFile(Path path) throws SerializationException {
        final List<Commit> revisions = loadParents();
        Commit parentWithThisFile = null;
        final String fileHash = getFileHash(path);
        for (final Commit revision : revisions) {
            if (revision.checkFileEquals(path, fileHash)) {
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
            os.writeObject(revHash);
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
            revHash = (String) os.readObject();
            index = Utils.deserializeMapWithPath(os, Path.class, Path.class);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    /**
     * Copies file from specified revision to the stage.
     *
     * @param revision revision with file.
     * @param path     path to the file.
     * @throws IOException if could not copy file.
     */
    private void copyFileFromRevision(Revision revision, Path path) throws IOException {
        final Path dir = Utils.getStageFiles(root);
        final Path location = revision.getRealFileLocation(path);
        Utils.copyToDir(location, dir);
        Files.copy(location, path, StandardCopyOption.REPLACE_EXISTING);
        index.put(path, dir.resolve(location.getFileName()));
    }

    /**
     * Copies all files from revision to the stage making them equal.
     *
     * @param revision revision to checkout.
     * @throws CheckoutException if could not copy files.
     */
    public void checkoutRevision(Commit revision) throws CheckoutException {
        revHash = null;
        try {
            for (final Path path : listFiles()) {
                removeIfExist(path);
            }
            index.clear();
            for (final Path path : revision.listFiles()) {
                copyFileFromRevision(revision, path);
            }
            setParents(Collections.singleton(revision.getRevHash()));
            revHash = revision.getFilesHash();
            branch = null;
        } catch (IOException e) {
            throw new CheckoutException("Could not copy files", e);
        }
    }

    /**
     * Merges stage with another commit. Resolving strategy works as follows:
     * <ul>
     * <li> Copy all files from stage to the new commit. </li>
     * <li> Copy all files from {@code commitToMergeWith} that do not exist in stage. </li>
     * <li> Commit result and update stage that new commit is a parent of updated stage. </li>
     * </ul>
     *
     * @param commitToMergeWith commit to merge with.
     * @return full hash of a new commit.
     * @throws MergeException if could not copy files or perform a commit.
     * @see Stage#commit()
     */
    public String merge(AbstractCommit commitToMergeWith) throws MergeException {
        revHash = null;
        final Set<Path> filesToMergeIn = commitToMergeWith.listFiles();
        try {
            for (final Path path : filesToMergeIn) {
                if (!checkFileInRevision(path)) {
                    copyFileFromRevision(commitToMergeWith, path);
                }
            }
        } catch (IOException e) {
            throw new MergeException("Failed to copy new files", e);
        }

        setParents(Stream.concat(getParents().stream(), Collections.singleton(commitToMergeWith.getRevHash()).stream())
                .collect(Collectors.toSet()));

        try {
            return commit();
        } catch (CommitException e) {
            throw new MergeException("Failed to commit merge", e);
        }
    }

    @Override
    public String getRevHash() {
        if (revHash == null) {
            revHash = super.getRevHash();
        }
        return revHash;
    }
}
