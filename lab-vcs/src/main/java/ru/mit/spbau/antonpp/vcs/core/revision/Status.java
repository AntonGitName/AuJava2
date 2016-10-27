package ru.mit.spbau.antonpp.vcs.core.revision;

import ru.mit.spbau.antonpp.vcs.core.exceptions.StatusReadingException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Status {

    private final Stage stage;
    private final Path workingDir;

    public List<Path> getUnchanged() {
        return unchanged;
    }

    public List<Path> getStageAdded() {
        return stageAdded;
    }

    public List<Path> getStageModified() {
        return stageModified;
    }

    public List<Path> getStageRemoved() {
        return stageRemoved;
    }

    private final List<Path> unchanged;
    private final List<Path> untracked;
    private final List<Path> stageAdded;
    private final List<Path> stageModified;
    private final List<Path> stageRemoved;
    private final List<Path> notStageModified = new ArrayList<>();
    private final List<Path> notStageRemoved = new ArrayList<>();


    public Status(Stage stage, Path workingDir) throws StatusReadingException {
        this.stage = stage;
        this.workingDir = workingDir;

        final Set<Path> revisionFiles = stage.getParent().listFiles();
        final Set<Path> stagedFiles = stage.listStagedFiles();
        final List<Path> allCurrentFiles;
        try {
            allCurrentFiles = Utils.listFilesRecursivelyExceptInternls(workingDir);


            untracked = allCurrentFiles.stream()
                    .filter(x -> !stagedFiles.contains(x))
                    .filter(x -> !revisionFiles.contains(x))
                    .collect(Collectors.toList());

            final List<Path> stageNotRemoved = stagedFiles.stream().filter(x -> stage.getFileHash(x) != null)
                    .collect(Collectors.toList());
            stageAdded = stageNotRemoved.stream().filter(x -> !revisionFiles.contains(x)).collect(Collectors.toList());
            stageModified = stageNotRemoved.stream().filter(revisionFiles::contains).collect(Collectors.toList());
            stageRemoved = stagedFiles.stream().filter(x -> stage.getFileHash(x) == null).collect(Collectors.toList());
            unchanged = revisionFiles.stream().filter(x -> !stagedFiles.contains(x)).collect(Collectors.toList());


            final Map<Path, String> currentOverall = new HashMap<>(allCurrentFiles.size());
            for (final Path path : allCurrentFiles) {
                currentOverall.put(path, Utils.getHashForFile(workingDir, path).toString());
            }

            final Map<Path, String> stageOverall = new HashMap<>();
            for (final Path path : unchanged) {
                stageOverall.put(path, stage.getParent().getFileHash(path));
            }
            for (final Path path : stageModified) {
                stageOverall.put(path, stage.getFileHash(path));
            }
            for (final Path path : stageAdded) {
                stageOverall.put(path, stage.getFileHash(path));
            }

            for (final Map.Entry<Path, String> pathHash : stageOverall.entrySet()) {
                final Path path = pathHash.getKey();
                final String hash = pathHash.getValue();
                if (currentOverall.containsKey(path)) {
                    if (!currentOverall.get(path).equals(hash)) {
                        notStageModified.add(path);
                    }
                } else {
                    notStageRemoved.add(path);
                }
            }


        } catch (IOException e) {
            throw new StatusReadingException("Could not read internal files.", e);
        }
    }

    @Override
    public String toString() {
        final Path dir = Utils.getCurrentDir();
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("Last revision md5: %s...\n", stage.getParent().getShortHash()));
        sb.append("Changes to be committed:\n\n");
        stageAdded.stream().map(dir::relativize).forEach(x -> sb.append(String.format("\tA\t%s\n", x)));
        stageModified.stream().map(dir::relativize).forEach(x -> sb.append(String.format("\tM\t%s\n", x)));
        stageRemoved.stream().map(dir::relativize).forEach(x -> sb.append(String.format("\tR\t%s\n", x)));
        sb.append("\nChanges not staged for commit:\n\n");
        notStageModified.stream().map(dir::relativize).forEach(x -> sb.append(String.format("\tM\t%s\n", x)));
        notStageRemoved.stream().map(dir::relativize).forEach(x -> sb.append(String.format("\tR\t%s\n", x)));
        sb.append("\nUntracked files:\n\n");
        untracked.stream().map(dir::relativize).forEach(x -> sb.append(String.format("\t\t%s\n", x)));
        return sb.toString();
    }
}
