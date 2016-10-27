package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StageAddException;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;

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
public class CommandRemove extends CommandWithRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandReset.class);

    @Parameter(description = "Files to remove")
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
                Files.delete(path);
                stage.addChangesToStage(path);
                LOGGER.debug("File {} was removed", path);
            } catch (StageAddException e) {
                final String msg = String.format("Could not remove file (%s) from index.", path);
                LOGGER.error(msg, e);
                System.out.println(msg);
                System.exit(1);
            } catch (IOException e) {
                final String msg = String.format("Could not remove file (%s) from disk.", path);
                LOGGER.error(msg, e);
                System.out.println(msg);
            }
        });

    }
}
