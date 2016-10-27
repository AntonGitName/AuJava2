package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StatusReadingException;
import ru.mit.spbau.antonpp.vcs.core.revision.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Parameters(commandNames = "clean", commandDescription = "Remove all not versioned files")
public class CommandClean extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandClean.class);

    @Override
    public void run() {
        super.run();
        try {
            final Status status = new Status(repository.getStage(), workingDir);
            final List<Path> untracked = status.getUntracked();
            for (final Path path : untracked) {
                Files.delete(path);
                LOGGER.debug("Removed untracked file: {}", path);
            }
        } catch (StatusReadingException e) {
            final String msg = "Failed to access repository status.";
            LOGGER.error(msg, e);
            System.out.println(msg);
            System.exit(1);
        } catch (IOException e) {
            final String msg = "Failed to delete files.";
            LOGGER.error(msg, e);
            System.out.println(msg);
            System.exit(1);
        }
    }
}
