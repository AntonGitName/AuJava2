package ru.mit.spbau.antonpp.vcs.core;

import com.google.common.hash.Hashing;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Stage {

    private final Revision parent;
    private final Path workingDir;
    private final Map<Path, Path> added;

    public Stage(Revision parent, Path workingDir) throws IOException {
        this.parent = parent;
        this.workingDir = workingDir;
        added = readIndex();
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

    public void add(Path path) throws IOException {
        final String hash = Utils.getHashForFile(workingDir, path).toString();
        final Path stagedPath = Utils.getStageDir(workingDir).resolveSibling(hash);
        Files.copy(path, stagedPath, StandardCopyOption.REPLACE_EXISTING);
        added.put(path, stagedPath);

        final byte[] bytes = String.format("%s %s\n", path, stagedPath).getBytes();
        Files.write(Utils.getStageIndex(workingDir), bytes, StandardOpenOption.APPEND);
    }

    public String getFileHash(Path path) {
        if (added.containsKey(path)) {
            return added.get(path).getFileName().toString();
        }
        return null;
    }

    public Set<Path> listAddedFiles() {
        return added.keySet();
    }

    public String commit() {
        final Set<Path> parentFiles = parent.listFiles();
        final Set<Path> stagedFiles = listAddedFiles();

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
                Files.move(added.get(path), target);
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
