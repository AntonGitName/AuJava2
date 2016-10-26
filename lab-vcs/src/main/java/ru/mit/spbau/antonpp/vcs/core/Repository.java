package ru.mit.spbau.antonpp.vcs.core;

import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.vcs.core.exceptions.FindRepositoryException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Repository {

    @NotNull
    private final Path workingDir;

    @NotNull
    private Revision head;

    @NotNull
    private Stage stage;

    public Repository(@NotNull Path workingDir) throws FindRepositoryException {
        this.workingDir = workingDir;
        head = getHead();
        try {
            stage = new Stage(head, workingDir);
        } catch (IOException e) {
            throw new FindRepositoryException("Internal files are corrupted. Failed to read staging area.", e);
        }
    }

    public Stage getStage() {
        return stage;
    }

    private Revision getHead() throws FindRepositoryException {
        final Path headHashPath = Utils.getHeadHashFile(workingDir);
        try {
            final String headHash = Utils.getFileContent(headHashPath);
            return new Revision(workingDir, headHash);
        } catch (IOException e) {
            throw new FindRepositoryException("Internal files are corrupted. Could not find HEAD.", e);
        }
    }

}
