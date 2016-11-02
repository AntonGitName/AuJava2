package ru.mit.spbau.antonpp.vcs.core.revision;

import com.google.common.hash.Hashing;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * @author antonpp
 * @since 28/10/16
 */
@Slf4j
abstract class AbstractRevision implements Revision {

    @Getter(lazy = true)
    @NotNull
    private final String revHash = calcRevHash();
    @Getter
    @Setter
    protected Path root;

    @NotNull
    private String calcRevHash() {
        final String joinedHash = listFiles().stream().sorted().map(this::getFileHash).collect(Collectors.joining());
        val hash = Hashing.md5().hashString(joinedHash).toString();
        log.debug("Calculated hash for revision {} in class {} ", hash, this.getClass().getSimpleName());
        return hash;
    }

    @Override
    public boolean checkFileInRevision(Path path) {
        return listFiles().contains(path);
    }

    @Override
    public boolean checkFileEquals(Path path, String hash) {
        return checkFileInRevision(path) && hash.equals(getFileHash(path));
    }
}
