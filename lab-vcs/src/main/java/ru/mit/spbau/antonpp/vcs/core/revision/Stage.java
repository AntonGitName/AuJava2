package ru.mit.spbau.antonpp.vcs.core.revision;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.*;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Stage extends AbstractRevision {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stage.class);


    private String parent;

    @Nullable
    private String branch;

    public static void main(String[] args) throws Exception {
//        Stage stage = new Stage();

        final String s = "/Users/antonpp/Documents/Projects/AuJava2/lab-vcs/build/libs/vcs-internals/stage/stage_dump";
        final String s1 = "/Users/antonpp/Documents/Projects/AuJava2/lab-vcs/build/libs/123.txt";
        final Path s2 = Paths.get("/Users/antonpp/Documents/Projects/AuJava2/lab-vcs/build/libs/tmp.txt");
//        stage.deserialize(Paths.get(s));
//        stage.addChanges(Paths.get(s1));
        Map<Path, Path> m = new HashMap<>();
        m.put(Paths.get(s), Paths.get(s1));
        final CommitInfo commitInfo = new CommitInfo(s1, s1, null);
        final CommitInfo commitInfo2 = new CommitInfo(s, s, null);
        final ArrayList<CommitInfo> list = new ArrayList<>();
        list.add(commitInfo);
        list.add(commitInfo2);
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(s2.toFile()))) {
            os.writeObject(list);
        }
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(s2.toFile()))) {

            System.out.println(os.readObject());
        }
    }

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
                throw new ResetException("File is not in stage and not versioned in HEAD. Cannot reset.");
            }
            try {
                Files.delete(getFileLocation(path));
            } catch (IOException e) {
                throw new ResetException("Could not remove file from stage", e);
            }
            index.remove(path);
        } else {
            final String fileHash = parentWithFile.getFileHash(path);
            try {
                Utils.copyToDir(parentWithFile.getFileLocation(path), Utils.getStageFiles(root));
            } catch (IOException e) {
                throw new ResetException("Could not copy file from parent revision", e);
            }
            index.put(path, Utils.getStageFiles(root).resolve(fileHash));
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
            throw new StageAddException("Could not add changes to stage", e);
        }

    }

    private void removeIfExist(Path path) throws IOException {
        if (index.containsKey(path)) {
            Files.delete(path);
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
        for (final Path path : files) {
            final Commit parentWithThisFile;
            try {
                parentWithThisFile = findParentWithExactFile(path);
            } catch (SerializationException e) {
                throw new CommitException("Failed to deserialize parent revision", e);
            }
            if (parentWithThisFile != null) {
                index.put(path, parentWithThisFile.getFileLocation(path));
            } else {
                try {
                    Utils.moveToDir(getFileLocation(path), Utils.getRevisionFiles(root, commitHash));
                } catch (IOException e) {
                    throw new CommitException("Failed to move file", e);
                }
            }
        }
        try {
            cloneToCommit().serialize(Utils.getRevisionIndex(root, commitHash));
        } catch (SerializationException e) {
            throw new CommitException("Failed to save commit", e);
        }
        return getRevHash();
    }

    private Commit cloneToCommit() {
        final Commit commit = new Commit();
        commit.setRoot(root);
        commit.setParents(getParents());
        commit.index = index;
        return commit;
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
            os.writeObject(parent);
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
            parent = (String) os.readObject();
            branch = (String) os.readObject();
//            index = (Map<Path, Path>) os.readObject();
            index = Utils.deserializeMapWithPath(os, Path.class, Path.class);
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    @Override
    public List<String> getParents() {
        if (parent == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(parent);
    }

    @Override
    public void setParents(List<String> parents) {
        if (parents.size() != 1) {
            throw new IllegalArgumentException("Stage can have only one parent");
        }
        this.parent = parents.get(0);
    }

    public void checkoutRevision(AbstractRevision revision) throws CheckoutException {
        try {
            for (final Path path : listFiles()) {
                removeIfExist(path);
            }
            index.clear();
            for (final Path path : revision.listFiles()) {
                final Path dir = Utils.getStageFiles(root);
                final Path location = revision.getFileLocation(path);
                Utils.copyToDir(location, dir);
                index.put(path, dir.resolve(location.getFileName()));
            }
            parent = revision.getRevHash();
            branch = null;
        } catch (IOException e) {
            throw new CheckoutException("Could not copy files", e);
        }
    }
}
