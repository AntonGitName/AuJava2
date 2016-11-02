package ru.mit.spbau.antonpp.vcs.core.utils;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
class Constants {

    static final String GLOBAL_INTERNALS = ".vcs-internals";
    static final String GLOBAL_LOGS = "log_dump";
    static final String GLOBAL_REPOSITORY = "repository_dump";
    static final String GLOBAL_REVISIONS = "revs";
    static final String GLOBAL_BRANCHES = "branches_dump";

    static final String REV_FILES = "files";
    static final String REV_INDEX = "rev_dump";

    static final String STAGE = "stage";
    static final String STAGE_INDEX = "stage_dump";
    static final String STAGE_FILES = "files";

    private Constants() {
    }
}
