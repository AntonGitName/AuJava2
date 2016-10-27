package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.NoSuchFileInRevisionException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.ResetException;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Parameters(commandNames = "reset", commandDescription = "Resets file to versioned state")
public class CommandReset extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandReset.class);

    @Parameter(description = "Files to reset")
    private List<String> files;

    @Override
    public void run() {
        super.run();
        LOGGER.debug("User specified files: {}", files);
        final Stage stage = repository.getStage();
        final List<Path> paths = files.stream().map(currentDir::resolve).collect(Collectors.toList());
        LOGGER.debug("Found paths: {}", paths);
        paths.forEach(path -> {
            try {
                stage.reset(path);
                LOGGER.debug("File {} was reseted", path);
            } catch (ResetException e) {
                final String msg = "Could not reset file";
                LOGGER.error(msg, e);
                System.out.println(msg);
                System.exit(1);
            } catch (NoSuchFileInRevisionException e) {
                final String msg = String.format("File (%s) was not found in last revision.", path);
                LOGGER.error(msg, e);
                System.out.println(msg);
            }
        });

    }
}
