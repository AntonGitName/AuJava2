package ru.mit.spbau.antonpp.vcs.core.revision;

import com.google.common.hash.Hashing;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Basic interface that describes a vcs revision
 *
 * @author antonpp
 * @since 28/10/16
 * @see Commit
 * @see Stage
 * @see WorkingDir
 */
public interface Revision {

    Logger LOGGER = LoggerFactory.getLogger(Revision.class);

    /**
     * Calculate md5 hash of file that exists in a revision.
     *
     * @param path path to file.
     * @return md5 hash.
     */
    @NotNull String getFileHash(Path path);

    /**
     * Get a location of the file from where it can be copied.
     *
     * @param path a path to file.
     * @return location of the saved version.
     */
    @NotNull Path getRealFileLocation(Path path);

    /**
     * List all files in the revision.
     *
     * @return set all files.
     */
    @NotNull Set<Path> listFiles();


    /**
     * Check if file is saved in the revision and it has the same hash.
     *
     * @param path path to file.
     * @param hash file hash.
     * @return true if file is saved in the revision and it has the same hash. False otherwise.
     */
    boolean checkFileEquals(Path path, String hash);

    /**
     * Checks if file with such path is saved in the revision.
     *
     * @param path path to file.
     * @return true if file is revisioned and false otherwise.
     */
    boolean checkFileInRevision(Path path);

    /**
     * Calculates overall hash of the revision. It must be calculated as described below:
     * <ul>
     * <li> List all files in sorted order. </li>
     * <li> Calculate hash of each file and join the result preserving the order. </li>
     * <li> Calculate hash of the joined string. The result is revision hash. </li>
     * </ul>
     * But implementation may very for different types of revisions. It is generally recommended to cache this
     * information.
     *
     * @return revision hash
     */
    default String getRevHash() {
        final String joinedHash = listFiles().stream().sorted().map(this::getFileHash).collect(Collectors.joining());
        val hash = Hashing.md5().hashString(joinedHash).toString();
        LOGGER.debug("Calculated hash for revision {} via default implementation", hash);
        return hash;
    }

}
