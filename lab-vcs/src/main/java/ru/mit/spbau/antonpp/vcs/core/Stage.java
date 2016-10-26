package ru.mit.spbau.antonpp.vcs.core;

import com.google.common.hash.Hashing;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StageAddException;
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

    public Revision getParent() {
        return parent;
    }

    private final Revision parent;
    private final Path workingDir;
    private final Map<Path, Path> staged;

    public Stage(Revision parent, Path workingDir) throws IOException {
        this.parent = parent;
        this.workingDir = workingDir;
        staged = readIndex();
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

    private void addFile(Path path) throws IOException {
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
                final Path stagedPath = Utils.getStageDir(workingDir).resolve(hash);
                Files.copy(path, stagedPath, StandardCopyOption.REPLACE_EXISTING);
                staged.put(path, stagedPath);
                writeIndexRecordsFromCurrentMap();
                LOGGER.debug("File added to stage ({})", path);
            }
        } else {
            final String revisionHash = parent.getFileHash(path);
            if (revisionHash == null || !revisionHash.equals(hash)) {
                final Path stagedPath = Utils.getStageDir(workingDir).resolveSibling(hash);
                Files.copy(path, stagedPath, StandardCopyOption.REPLACE_EXISTING);
                staged.put(path, stagedPath);
                writeIndexRecordsFromCurrentMap();
                LOGGER.debug("File added to stage ({})", path);
            } else {
                LOGGER.warn("Adding unchanged file");
            }
        }
    }

    public void addChangesToStage(Path path) throws StageAddException {
        final boolean toRemove = !Files.exists(path);
        try {
            if (toRemove) {
                removeFile(path);
            } else {
                addFile(path);
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

    public String commit() {
        final Set<Path> parentFiles = parent.listFiles();
        final Set<Path> stagedFiles = listStagedFiles();

        final List<Path> newFiles = stagedFiles.stream().filter(path -> parent.getFileHash(path) == null)
                .collect(Collectors.toList());

        final List<Path> changedFiles = stagedFiles.stream().filter(path -> {
            final String parentFileHash = parent.getFileHash(path);
            return parentFileHash != null && !parentFileHash.equals(getFileHash(path));
        }).collect(Collectors.toList());

        final List<Path> unchangedFiles = stagedFiles.stream().filter(path -> {
            final String parentFileHash = parent.getFileHash(path);
            return parentFileHash != null && parentFileHash.equals(getFileHash(path));
        }).collect(Collectors.toList());

        final Map<Path, String> rightHash = new HashMap<>();
        final StringBuilder sb = new StringBuilder();
        changedFiles.forEach(x -> rightHash.put(x, getFileHash(x)));
        newFiles.forEach(x -> rightHash.put(x, getFileHash(x)));
        unchangedFiles.forEach(x -> rightHash.put(x, parent.getFileHash(x)));

        final List<Path> allFiles = new ArrayList<>(unchangedFiles);
        allFiles.addAll(changedFiles);
        allFiles.addAll(newFiles);
        Collections.sort(allFiles);

        final String joinedHash = allFiles.stream().map(rightHash::get).collect(Collectors.joining());
        final String revisionHash = Hashing.md5().hashString(joinedHash).toString();

        final List<Path> stagedFilesToCopy = new ArrayList<>(changedFiles);
        stagedFilesToCopy.addAll(newFiles);

        try {
            Path revisionParents = Utils.getRevisionParents(workingDir, revisionHash);
            Path revisionIndex = Utils.getRevisionIndex(workingDir, revisionHash);
            Path revisionFiles = Utils.getRevisionFiles(workingDir, revisionHash);

            Files.createDirectories(revisionIndex);
            Files.createDirectories(revisionParents);
            Files.createDirectories(revisionFiles);

            try (final PrintWriter out = new PrintWriter(revisionParents.toFile())) {
                out.println(parent.getHash());
            }

            final Map<Path, Path> newPaths = new HashMap<>(stagedFilesToCopy.size());
            for (final Path path : stagedFilesToCopy) {
                final Path target = revisionFiles.resolve(path.getFileName());
                Files.move(staged.get(path), target);
                newPaths.put(path, target);
            }

            try (final PrintWriter out = new PrintWriter(revisionIndex.toFile())) {
                for (final Path parentFile : parentFiles) {
                    out.printf("%s %s\n", parentFile, parent.getFileLocation(parentFile));
                }
                for (final Path stagedFileToCopy : stagedFilesToCopy) {
                    out.printf("%s %s\n", stagedFileToCopy, newPaths.get(stagedFileToCopy));
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to commit. Possibly internal files are corrupted.", e);
        }


        return revisionHash;
    }

}
