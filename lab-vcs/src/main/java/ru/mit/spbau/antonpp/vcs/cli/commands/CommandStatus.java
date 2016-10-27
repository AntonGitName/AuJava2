package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StatusReadingException;
import ru.mit.spbau.antonpp.vcs.core.revision.Status;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = {"st", "status"}, commandDescription = "Print status")
public class CommandStatus extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandStatus.class);

    @Override
    public void run() {
        super.run();
        try {
            System.out.println(new Status(repository.getStage(), workingDir).toString());
        } catch (StatusReadingException e) {
            final String msg = "Failed to evaluate status.";
            LOGGER.error(msg, e);
            System.out.println(msg);
            System.exit(1);
        }

    }
}
