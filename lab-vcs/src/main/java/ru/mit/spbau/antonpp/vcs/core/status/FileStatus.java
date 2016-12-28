package ru.mit.spbau.antonpp.vcs.core.status;

/**
 * All possible states of tracked files in revision.
 *
 * @author antonpp
 * @since 27/10/16
 * @see Status
 */
public enum FileStatus {
    ADDED, MODIFIED, REMOVED, UNCHANGED;

    public String getName() {
        return getShortName() + toString().substring(1).toLowerCase();
    }

    public String getShortName() {
        return toString().substring(0, 1);
    }
}
