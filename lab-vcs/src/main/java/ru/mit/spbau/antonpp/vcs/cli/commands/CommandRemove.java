package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StageAddException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
@Parameters(commandNames = "rm", commandDescription = "Remove file from filesystem and index")
public class CommandRemove extends AbstractCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandReset.class);

    @Parameter(description = "Files to remove")
    private List<String> files;

    public CommandRemove() {
        super(true);
    }

    @Override
    public void runInternal() {
        LOGGER.debug("User specified files: {}", files);

        final List<Path> paths = files.stream().map(currentDir::resolve).collect(Collectors.toList());
        LOGGER.debug("Found paths: {}", paths);
        paths.forEach(path -> {
            try {
                Files.delete(path);
                repository.addChanges(path);
                LOGGER.debug("File {} was removed", path);
            } catch (StageAddException e) {
                final String msg = String.format("Could not remove file (%s) from index.", path);
                exitWithError(e, msg);
            } catch (IOException e) {
                final String msg = String.format("Could not remove file (%s) from disk.", path);
                exitWithError(e, msg);
            } catch (SerializationException e) {
                exitWithError(e, e.getMessage());
            }
        });

    }
}
