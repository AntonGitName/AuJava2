package ru.mit.spbau.antonpp.vcs.core.revision;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

/**
 * This class add to {@link Revision} interface two things: caching of revision hash and path to the root of repository.
 *
 * @author antonpp
 * @since 28/10/16
 */
abstract class AbstractRevision implements Revision {

    @Getter @Setter
    protected Path root;

    @Override
    public boolean checkFileInRevision(Path path) {
        return listFiles().contains(path);
    }

    @Override
    public boolean checkFileEquals(Path path, String hash) {
        return checkFileInRevision(path) && hash.equals(getFileHash(path));
    }
}
