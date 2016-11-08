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
 * Class that imitates a revision for all current files in working directory. Includes unrevisioned files.
 *
 * @author antonpp
 * @since 28/10/16
 */
@Slf4j
public final class WorkingDir extends AbstractRevision {

    public WorkingDir(@NotNull Path root) {
        this.root = root;
    }

    /**
     * Unlike for revisioned files calculates file's hash without any caching.
     *
     * @param path path to file.
     * @return file's md5 hash.
     */
    @Override
    public @NotNull String getFileHash(Path path) {
        try {
            return Utils.getFileHash(root, path);
        } catch (IOException e) {
            throw new HashCalculationException(e);
        }
    }

    @Override
    @NotNull
    public Path getRealFileLocation(Path path) {
        return path;
    }

    /**
     * Lists all files in repository except internal vcs files.
     *
     * @return list of all files.
     */
    @Override
    @NotNull
    public Set<Path> listFiles() {
        return new HashSet<>(Utils.listFilesRecursivelyExceptInternls(root));
    }

    @Override
    public boolean checkFileEquals(Path path, String hash) {
        return Files.exists(path) && getFileHash(path).equals(hash);
    }

}
