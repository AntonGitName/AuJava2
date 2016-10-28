package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = {"st", "status"}, commandDescription = "Print status")
public class CommandStatus extends AbstractCommand {

    public CommandStatus() {
        super(true);
    }

    @Override
    public void runInternal() {
        try {
            System.out.println(repository.status());
        } catch (SerializationException e) {
            exitWithError(e, "Failed to load revisions");
        }
    }
}
