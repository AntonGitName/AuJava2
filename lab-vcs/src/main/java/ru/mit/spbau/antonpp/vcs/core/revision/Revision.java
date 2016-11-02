package ru.mit.spbau.antonpp.vcs.core.revision;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;

/**
 * @author antonpp
 * @since 28/10/16
 */
public interface Revision {

    @NotNull String getFileHash(Path path);


    Path getRealFileLocation(Path path);


    Set<Path> listFiles();


    boolean checkFileEquals(Path path, String hash);

    boolean checkFileInRevision(Path path);


    String getRevHash();

}
