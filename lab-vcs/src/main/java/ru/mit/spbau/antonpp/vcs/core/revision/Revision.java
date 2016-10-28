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


    Path getFileLocation(Path path);


    Set<Path> listFiles();


    boolean checkFile(Path path, String hash);


    @NotNull String getRevHash();

}
