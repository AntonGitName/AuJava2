package ru.mit.spbau.antonpp.vcs.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Repository {

    private static final String DEFAULT_WORK_DIR = "./project";
    private static final String DEFAULT_INTERNALS = "/.vcs-internals";

    private final String workingDir;
    private final String internalsDir;

    private final List<String> index = new ArrayList<>();

    public Repository(String workingDir) {
        this.workingDir = workingDir;
        internalsDir = workingDir + DEFAULT_INTERNALS;
    }

}
