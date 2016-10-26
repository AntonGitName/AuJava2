package ru.mit.spbau.antonpp.vcs.core;

import org.jetbrains.annotations.Nullable;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Revision {

    private final String hash;
    private final List<String> parents;
    private final Map<Path, Path> files;

    public Revision(Path workingDir, String hash) throws IOException {
        this(workingDir, Utils.getRevisionDir(workingDir, hash));
    }

    public Revision(Path workingDir, Path pathToRevision) throws IOException {
        hash = pathToRevision.getFileName().toString();

        final Path parentsPath = Utils.getRevisionParents(workingDir, hash);
        try (Stream<String> stream = Files.lines(parentsPath)) {
            parents = stream.collect(Collectors.toList());
        }

        final Path indexPath = Utils.getRevisionIndex(workingDir, hash);
        try (Stream<String> stream = Files.lines(indexPath)) {
            files = stream.map(line -> line.split(" ")).collect(Collectors.toMap(kv -> Paths.get(kv[0]), kv -> Paths.get(kv[1])));
        }
    }

    @Nullable
    public String getFileHash(Path path) {
        if (files.containsKey(path)) {
            return files.get(path).getFileName().toString();
        }
        return null;
    }

    public Path getFileLocation(Path path) {
        return files.get(path);
    }

    public Set<Path> listFiles() {
        return files.keySet();
    }

    public void checkout() throws IOException {
        for (Map.Entry<Path, Path> kv : files.entrySet()) {
            final Path whereToPlace = kv.getKey();
            final Path whereToTake = kv.getValue();
            Files.copy(whereToTake, whereToPlace, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public String getHash() {
        return hash;
    }

    public String getShortHash() {
        return hash.substring(0, 6) + "...";
    }
}
