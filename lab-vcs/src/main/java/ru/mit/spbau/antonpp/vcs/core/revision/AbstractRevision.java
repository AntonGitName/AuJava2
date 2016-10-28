package ru.mit.spbau.antonpp.vcs.core.revision;

import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mit.spbau.antonpp.vcs.core.FileSerializable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author antonpp
 * @since 28/10/16
 */
public abstract class AbstractRevision implements Revision, FileSerializable {

    @Nullable
    protected String revHash;
    protected Path root;
    protected Map<Path, Path> index = new HashMap<>();

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    public abstract List<String> getParents();

    public abstract void setParents(List<String> parents);

    @NotNull
    public String getFileHash(Path path) {
        if (index.containsKey(path)) {
            return index.get(path).getFileName().toString();
        }
        throw new IllegalArgumentException("Specified file was not found in the revision.");
    }

    public Path getFileLocation(Path path) {
        return index.get(path);
    }

    public Set<Path> listFiles() {
        return index.keySet();
    }

    public boolean checkFile(Path path) {
        return index.containsKey(path);
    }

    public boolean checkFile(Path path, String hash) {
        return index.containsKey(path) && hash.equals(getFileHash(path));
    }

    @NotNull
    public String getRevHash() {
        if (revHash == null) {
            revHash = calcRevHash();
        }
        return revHash;
    }

    public String getShortHash() {
        return getRevHash().substring(0, 6);
    }

    private String calcRevHash() {
        final List<Path> files = new ArrayList<>(listFiles());
        final String joinedHash = files.stream().sorted().map(this::getFileHash).collect(Collectors.joining());
        return Hashing.md5().hashString(joinedHash).toString();
    }
}
