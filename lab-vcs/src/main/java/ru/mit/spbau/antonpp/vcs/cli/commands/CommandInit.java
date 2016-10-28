package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.Repository;
import ru.mit.spbau.antonpp.vcs.core.exceptions.InitException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Path;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "init", commandDescription = "Create index")
public class CommandInit extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInit.class);

    public CommandInit() {
        super(false);
    }

    @Override
    public void runInternal() {
        final Path internals = Utils.getInternals(currentDir);

        LOGGER.debug("Internals path: {}", internals);

        try {
            Repository.init();
        } catch (InitException e) {
            exitWithError(e, "Failed to create repository in this folder.");
        }
    }
}
