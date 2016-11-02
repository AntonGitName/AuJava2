package ru.mit.spbau.antonpp.vcs.cli.commands;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.mit.spbau.antonpp.vcs.core.Repository;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Path;

/**
 * This class is used to reduce the amount of boilerplate code that loads repository and initializes path variables.
 *
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Slf4j
public abstract class AbstractCommand {

    private final boolean needRepository;

    protected Repository repository;
    protected Path currentDir;
    private Path root;

    /**
     * Specifies if repository should be loaded.
     *
     * @param needRepository true if needed for a subclass
     */
    protected AbstractCommand(boolean needRepository) {
        this.needRepository = needRepository;
    }

    public final void run() {
        preRun();
        runInternal();
        postRun();
    }

    /**
     * Releases used resources.
     */
    private void postRun() {
        if (needRepository) {
            try {
                repository.serialize(Utils.getRepository(root));
            } catch (SerializationException e) {
                exitWithError(e, "Could not save repository state");
            }
            log.debug("Repository saved");
        }
        // no need to serialize head
    }

    /**
     * Acquires resources.
     */
    private void preRun() {
        root = Utils.getRoot();
        currentDir = Utils.getCurrentDir();
        if (needRepository) {
            if (root == null) {
                final String msg = "Could not found repository root.";
                exitWithError(null, msg);
            }
            log.debug("Found root at {}", root);
            try {
                repository = new Repository();
                repository.deserialize(Utils.getRepository(root));
            } catch (SerializationException e) {
                exitWithError(e, "Could not load repository files.");
            }
            log.debug("Repository loaded");
        }
    }

    protected void exitWithError(@Nullable Throwable e, String msg) {
        if (e != null) {
            log.error(msg, e);
        } else {
            log.error(msg);
        }
        System.out.println(msg);
        System.exit(1);
    }

    /**
     * Any subclass must override this method and define all useful work there because it is only place where
     * {@link Repository} instance is available.
     */
    protected abstract void runInternal();
}
