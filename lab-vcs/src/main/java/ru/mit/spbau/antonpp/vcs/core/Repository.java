package ru.mit.spbau.antonpp.vcs.core;

import org.jetbrains.annotations.NotNull;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CheckoutIOException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CheckoutStageNotClearException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.RevisionCheckoutException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StatusReadingException;
import ru.mit.spbau.antonpp.vcs.core.revision.Revision;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
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

    public Repository(@NotNull Path workingDir) throws RevisionCheckoutException {
        this.workingDir = workingDir;
        head = initHead();
        stage = new Stage(head, workingDir);
    }

    public Stage getStage() {
        return stage;
    }

    private Revision initHead() throws RevisionCheckoutException {
        final Path headHashPath = Utils.getHeadHashFile(workingDir);
        final String headHash;
        try {
            headHash = Utils.getFileContent(headHashPath);
        } catch (IOException e) {
            throw new RevisionCheckoutException("Internal files are corrupted. Could not find HEAD.", e);
        }
        return new Revision(workingDir, headHash);
    }

    public void checkout(String hash) throws CheckoutStageNotClearException, StatusReadingException,
            RevisionCheckoutException, CheckoutIOException {
        if (!stage.isClear()) {
            throw new CheckoutStageNotClearException();
        }
        head = new Revision(workingDir, hash);
        stage = new Stage(head, workingDir);
        try {
            Files.write(Utils.getHeadHashFile(workingDir), hash.getBytes());
        } catch (IOException e) {
            throw new CheckoutIOException("Failed to save head record", e);
        }
    }

}
