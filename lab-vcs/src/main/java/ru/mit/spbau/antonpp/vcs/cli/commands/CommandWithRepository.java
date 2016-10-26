package ru.mit.spbau.antonpp.vcs.cli.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.Repository;
import ru.mit.spbau.antonpp.vcs.core.exceptions.FindRepositoryException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Path;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public abstract class CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandWithRepository.class);

    protected Repository repository;
    protected Path currentDir;
    protected Path workingDir;

    public void run() {
        workingDir = Utils.getWorkingDir();
        currentDir = Utils.getCurrentDir();
        if (workingDir == null) {
            final String msg = "Could not found repository root.";
            LOGGER.error(msg);
            System.out.println(msg);
            System.exit(1);
        } else {
            LOGGER.debug("Found root at {}", workingDir);
            try {
                repository = new Repository(workingDir);
            } catch (FindRepositoryException e) {
                final String msg = "Could not load repository files.";
                LOGGER.error(msg, e);
                System.out.println(msg);
                System.exit(1);
            }
        }
    }
}
