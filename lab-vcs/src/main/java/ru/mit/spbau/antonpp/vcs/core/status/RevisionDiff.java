package ru.mit.spbau.antonpp.vcs.core.status;

import com.google.common.collect.Sets;
import ru.mit.spbau.antonpp.vcs.core.revision.Revision;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author antonpp
 * @since 27/10/16
 */
public class RevisionDiff {

    private final Map<Path, FileStatus> files = new HashMap<>();


    public RevisionDiff(Revision oldRev, Revision newRev) {

        final Set<Path> oldFiles = oldRev.listFiles();
        final Set<Path> newFiles = newRev.listFiles();

        Sets.difference(oldFiles, newFiles).forEach(x -> files.put(x, FileStatus.REMOVED));
        Sets.difference(newFiles, oldFiles).forEach(x -> files.put(x, FileStatus.ADDED));
        Sets.intersection(newFiles, oldFiles).stream().filter(x -> !oldRev.getFileHash(x).equals(newRev.getFileHash(x)))
                .forEach(x -> files.put(x, FileStatus.MODIFIED));
        Sets.intersection(newFiles, oldFiles).stream().filter(x -> oldRev.getFileHash(x).equals(newRev.getFileHash(x)))
                .forEach(x -> files.put(x, FileStatus.UNCHANGED));
    }


    public Map<Path, FileStatus> getFiles() {
        return Collections.unmodifiableMap(files);
    }
}
