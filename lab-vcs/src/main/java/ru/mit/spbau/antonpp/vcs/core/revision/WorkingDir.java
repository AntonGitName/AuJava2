package ru.mit.spbau.antonpp.vcs.core.revision;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.vcs.core.exceptions.HashCalculationException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * @author antonpp
 * @since 28/10/16
 */
@Slf4j
public final class WorkingDir extends AbstractRevision {

    @NotNull
    private final Path root;

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
    public Path getRealFileLocation(Path path) {
        return path;
    }

    @Override
    public Set<Path> listFiles() {
        return new HashSet<>(Utils.listFilesRecursivelyExceptInternls(root));
    }

    @Override
    public boolean checkFileEquals(Path path, String hash) {
        return Files.exists(path) && getFileHash(path).equals(hash);
    }

}
