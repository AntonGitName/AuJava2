package ru.mit.spbau.antonpp.vcs.core.revision;

import com.google.common.hash.Hashing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.HashCalculationException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author antonpp
 * @since 28/10/16
 */
public class WorkingDir implements Revision {

    @NotNull
    private final Path root;
    @Nullable
    protected String revHash;

    public WorkingDir(@NotNull Path root) {
        this.root = root;
    }

    @Override
    public @NotNull String getFileHash(Path path) {
        try {
            return Utils.getFileHash(root, path);
        } catch (IOException e) {
            throw new HashCalculationException(e);
        }
    }

    @Override
    public Path getFileLocation(Path path) {
        return path;
    }

    @Override
    public Set<Path> listFiles() {
        return new HashSet<>(Utils.listFilesRecursivelyExceptInternls(root));
    }

    @Override
    public boolean checkFile(Path path, String hash) {
        return Files.exists(path) && getFileHash(path).equals(hash);
    }

    @Override
    public @NotNull String getRevHash() {
        final List<Path> files = new ArrayList<>(listFiles());
        final String joinedHash = files.stream().sorted().map(this::getFileHash).collect(Collectors.joining());
        return Hashing.md5().hashString(joinedHash).toString();
    }


}
