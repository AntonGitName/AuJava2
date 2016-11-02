package ru.mit.spbau.antonpp.vcs.core.revision;

import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.vcs.core.FileSerializable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author antonpp
 * @since 02/11/2016
 */
abstract class AbstractCommit extends AbstractRevision implements FileSerializable {

    protected Map<Path, Path> index = new HashMap<>();
    @Setter
    protected Set<String> parents;

    public Set<String> getParents() {
        return parents != null ? parents : Collections.emptySet();
    }

    @Override
    @NotNull
    public String getFileHash(Path path) {
        if (index.containsKey(path)) {
            return index.get(path).getFileName().toString();
        }
        throw new IllegalArgumentException("Specified file was not found in the revision.");
    }

    @Override
    public Path getRealFileLocation(Path path) {
        return index.get(path);
    }

    @Override
    public Set<Path> listFiles() {
        return index.keySet();
    }
}
