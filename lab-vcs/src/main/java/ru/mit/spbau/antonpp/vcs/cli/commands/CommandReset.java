package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.ResetException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Parameters(commandNames = "reset", commandDescription = "Resets file to versioned state")
public class CommandReset extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandReset.class);

    @Parameter(description = "Files to reset")
    private List<String> files;

    public CommandReset() {
        super(true);
    }

    @Override

    public void runInternal() {
        LOGGER.debug("User specified files: {}", files);
        final List<Path> paths = files.stream().map(currentDir::resolve).collect(Collectors.toList());
        LOGGER.debug("Found paths: {}", paths);
        paths.forEach(path -> {
            try {
                repository.reset(path);
                LOGGER.debug("File {} was reseted", path);
            } catch (ResetException | SerializationException e) {
                exitWithError(e, "Could not reset file");
            }
        });

    }
}
