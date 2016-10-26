package ru.mit.spbau.antonpp.vcs.core;

import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Repository {

    private final Path workingDir;

    private final List<String> index = new ArrayList<>();

    @NotNull
    private Revision head;

    @NotNull
    private Stage stage;

    public Repository(Path workingDir) {
        this.workingDir = workingDir;
        head = getHead();
        try {
            stage = new Stage(head, workingDir);
        } catch (IOException e) {
            throw new RuntimeException("Internal files are corrupted. Failed to read staging area.", e);
        }
    }

    private Revision getHead() {
        final Path headHashPath = Utils.getHeadHashFile(workingDir);
        try {
            final String headHash = Utils.getFileContent(headHashPath);
            return new Revision(workingDir, headHash);
        } catch (IOException e) {
            throw new RuntimeException("Internal files are corrupted. Could not find HEAD.", e);
        }
    }

}
