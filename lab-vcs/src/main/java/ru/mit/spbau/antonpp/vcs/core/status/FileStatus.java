package ru.mit.spbau.antonpp.vcs.core.status;

/**
 * @author antonpp
 * @since 27/10/16
 */
public enum FileStatus {
    ADDED, MODIFIED, REMOVED, UNCHANGED, UNTRACKED;

    public String getName() {
        return getShortName() + toString().substring(1).toLowerCase();
    }

    public String getShortName() {
        return toString().substring(0, 1);
    }
}
