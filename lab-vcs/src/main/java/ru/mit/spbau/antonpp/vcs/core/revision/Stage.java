package ru.mit.spbau.antonpp.vcs.core.revision;

import com.google.common.hash.Hashing;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.*;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Stage {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stage.class);
    private final Revision parent;
    private final Path workingDir;
    private final Map<Path, Path> staged;

    public Stage(Revision parent, Path workingDir) throws RevisionCheckoutException {
        this.parent = parent;
        this.workingDir = workingDir;
        try {
            staged = readIndex();
        } catch (IOException e) {
            throw new RevisionCheckoutException("Could not load stage files", e);
        }
    }

    public Revision getParent() {
        return parent;
    }

    public boolean isClear() throws StatusReadingException {
        final Status status = new Status(this, workingDir);

        LOGGER.debug("Stage status:");
        LOGGER.debug("Added: {}", status.getStageAdded());
        LOGGER.debug("Modified: {}", status.getStageModified());
        LOGGER.debug("Removed: {}", status.getStageRemoved());

        return status.getStageAdded().isEmpty()
                && status.getStageModified().isEmpty()
                && status.getStageRemoved().isEmpty();
    }

    private Map<Path, Path> readIndex() throws IOException {
        final Map<Path, Path> result = new HashMap<>();
        final Path stageIndex = Utils.getStageIndex(workingDir);
        final List<String> lines = Files.readAllLines(stageIndex);
        for (final String line : lines) {
            final String[] splittedLine = line.split(" ");
            result.put(Paths.get(splittedLine[0]), Paths.get(splittedLine[1]));
        }
        return result;
    }

    private List<String> readIndexRecords() throws IOException {
        return Files.readAllLines(Utils.getStageIndex(workingDir));
    }

    private void writeIndexRecordsFromCurrentMap() throws FileNotFoundException {
        writeIndexRecords(staged.entrySet().stream()
                .map(kv -> String.format("%s %s", kv.getKey(), kv.getValue()))
                .collect(Collectors.toList()));
    }

    private void writeIndexRecords(List<String> index) throws FileNotFoundException {
        try (final PrintWriter out = new PrintWriter(Utils.getStageIndex(workingDir).toFile())) {
            index.forEach(out::println);
        }
    }

    private void removeFile(Path path) throws IOException {
        if (staged.containsKey(path)) {
            final Path realPath = staged.get(path);
            if (realPath == null) {
                LOGGER.warn("File was already removed from stage");
            } else {
                Files.delete(realPath);
                staged.put(path, null);
                writeIndexRecordsFromCurrentMap();
                LOGGER.debug("File marked for removal ({})", path);
            }
        } else {
            String revisionHash = parent.getFileHash(path);
            if (revisionHash == null) {
                LOGGER.warn("File was already removed from stage and revision");
            } else {
                staged.put(path, null);
                writeIndexRecordsFromCurrentMap();
                LOGGER.debug("File marked for removal ({})", path);
            }
        }
    }

    private void copyFileToStageDir(Path path, String hash) throws IOException {
        final Path stagedPath = Utils.getStageFiles(workingDir).resolve(hash);
        Files.copy(path, stagedPath, StandardCopyOption.REPLACE_EXISTING);
        staged.put(path, stagedPath);
        writeIndexRecordsFromCurrentMap();
        LOGGER.debug("File added to stage ({})", path);
    }

    private void addOrModifyFile(Path path) throws IOException {
        final String hash = Utils.getHashForFile(workingDir, path).toString();
        if (staged.containsKey(path)) {
            final String stagedHash = getFileHash(path);
            if (stagedHash != null && stagedHash.equals(hash)) {
                LOGGER.warn("Adding unchanged file");
            } else {
                if (stagedHash != null) {
                    final Path realPath = staged.get(path);
                    Files.delete(realPath);
                }
                final String revisionHash = parent.getFileHash(path);
                if (revisionHash != null && revisionHash.equals(hash)) {
                    LOGGER.debug("Removing file from stage because at is the same as in the last revision");
                    staged.remove(path);
                    writeIndexRecordsFromCurrentMap();
                } else {
                    copyFileToStageDir(path, hash);
                }
            }
        } else {
            final String revisionHash = parent.getFileHash(path);
            if (revisionHash == null || !revisionHash.equals(hash)) {
                copyFileToStageDir(path, hash);
            } else {
                LOGGER.warn("Adding unchanged file. Nothing to do");
            }
        }
    }

    public void reset(Path path) throws NoSuchFileInRevisionException, ResetException {
        if (parent.getFileHash(path) == null) {
            throw new NoSuchFileInRevisionException();
        }
        try {
            Files.copy(parent.getFileLocation(path), path, StandardCopyOption.REPLACE_EXISTING);
            addChangesToStage(path);
        } catch (IOException | StageAddException e) {
            throw new ResetException(e);
        }
    }

    public void addChangesToStage(Path path) throws StageAddException {
        final boolean toRemove = !Files.exists(path);
        try {
            if (toRemove) {
                removeFile(path);
            } else {
                addOrModifyFile(path);
            }
        } catch (IOException e) {
            throw new StageAddException("Could not read/write files.", e);
        }
    }

    @Nullable
    public String getFileHash(Path path) {
        if (staged.containsKey(path)) {
            if (staged.get(path) != null) {
                return staged.get(path).getFileName().toString();
            } else {
                return null;
            }
        }
        throw new IllegalArgumentException("this file is not in stage");
    }

    public Set<Path> listStagedFiles() {
        return staged.keySet();
    }

    public String commit() throws StatusReadingException, CommitException {
        final Status status = new Status(this, workingDir);

        final List<Path> unchanged = status.getUnchanged();
        final List<Path> stageAdded = status.getStageAdded();
        final List<Path> stageModified = status.getStageModified();

        final Map<Path, String> rightHash = new HashMap<>();
        stageModified.forEach(x -> rightHash.put(x, getFileHash(x)));
        stageAdded.forEach(x -> rightHash.put(x, getFileHash(x)));
        unchanged.forEach(x -> rightHash.put(x, parent.getFileHash(x)));

        final List<Path> allFilesSorted = new ArrayList<>(unchanged);
        allFilesSorted.addAll(stageAdded);
        allFilesSorted.addAll(stageModified);
        Collections.sort(allFilesSorted);

        final String joinedHash = allFilesSorted.stream().map(rightHash::get).collect(Collectors.joining());
        final String revisionHash = Hashing.md5().hashString(joinedHash).toString();

        final List<Path> stagedFilesToCopy = new ArrayList<>(stageAdded);
        stagedFilesToCopy.addAll(stageModified);

        try {
            final Path revisionParents = Utils.getRevisionParents(workingDir, revisionHash);
            final Path revisionIndex = Utils.getRevisionIndex(workingDir, revisionHash);
            final Path revisionFiles = Utils.getRevisionFiles(workingDir, revisionHash);

            Files.createDirectories(revisionFiles);
            Files.createFile(revisionIndex);
            Files.createFile(revisionParents);

            try (final PrintWriter out = new PrintWriter(revisionParents.toFile())) {
                out.println(parent.getHash());
            }

            final Map<Path, Path> newPaths = new HashMap<>(stagedFilesToCopy.size());
            for (final Path path : stagedFilesToCopy) {
                final Path target = revisionFiles.resolve(staged.get(path).getFileName());
                Files.move(staged.get(path), target);
                newPaths.put(path, target);
            }

            try (final PrintWriter out = new PrintWriter(revisionIndex.toFile())) {
                for (final Path parentFile : unchanged) {
                    out.printf("%s %s\n", parentFile, parent.getFileLocation(parentFile));
                }
                for (final Path stagedFileToCopy : stagedFilesToCopy) {
                    out.printf("%s %s\n", stagedFileToCopy, newPaths.get(stagedFileToCopy));
                }
            }

            staged.clear();
            writeIndexRecordsFromCurrentMap();

        } catch (IOException e) {
            throw new CommitException("Failed to commit. Possibly internal files are corrupted.", e);
        }


        return revisionHash;
    }

}
