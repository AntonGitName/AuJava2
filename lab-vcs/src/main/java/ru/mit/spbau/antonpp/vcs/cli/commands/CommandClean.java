package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.io.IOException;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Parameters(commandNames = "clean", commandDescription = "Remove all not versioned files")
public class CommandClean extends AbstractCommand {

    public CommandClean() {
        super(true);
    }

    @Override
    public void runInternal() {
        try {
            repository.clean();
        } catch (SerializationException | IOException e) {
            exitWithError(e, "Failed to delete files.");
        }
    }
}
