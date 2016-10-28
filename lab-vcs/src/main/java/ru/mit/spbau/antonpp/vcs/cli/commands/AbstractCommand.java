package ru.mit.spbau.antonpp.vcs.cli.commands;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.Repository;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Path;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public abstract class AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommand.class);

    private final boolean needRepository;

    protected Repository repository;
    protected Path currentDir;
    protected Path root;

    protected AbstractCommand(boolean needRepository) {
        this.needRepository = needRepository;
    }

    public final void run() {
        preRun();
        runInternal();
        postRun();
    }

    private void postRun() {
        if (needRepository) {
            try {
                repository.serialize(Utils.getRepository(root));
            } catch (SerializationException e) {
                exitWithError(e, "Could not save repository state");
            }
            LOGGER.debug("Repository saved");
        }
        // no need to serialize head
    }

    private void preRun() {
        root = Utils.getRoot();
        currentDir = Utils.getCurrentDir();
        if (needRepository) {
            if (root == null) {
                final String msg = "Could not found repository root.";
                exitWithError(null, msg);
            }
            LOGGER.debug("Found root at {}", root);
            try {
                repository = new Repository();
                repository.deserialize(Utils.getRepository(root));
            } catch (SerializationException e) {
                exitWithError(e, "Could not load repository files.");
            }
            LOGGER.debug("Repository loaded");
        }
    }

    protected void exitWithError(@Nullable Throwable e, String msg) {
        if (e != null) {
            LOGGER.error(msg, e);
        } else {
            LOGGER.error(msg);
        }
        System.out.println(msg);
        System.exit(1);
    }

    protected abstract void runInternal();
}