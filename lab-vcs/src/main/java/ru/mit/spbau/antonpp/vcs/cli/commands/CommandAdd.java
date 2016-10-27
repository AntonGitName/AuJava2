package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StageAddException;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "add", commandDescription = "Add file contents to the index")
public class CommandAdd extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandAdd.class);

    @Parameter(description = "Files to add to the index")
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
                stage.addChangesToStage(path);
                LOGGER.debug("File {} added to stage", path);
            } catch (StageAddException e) {
                final String msg = "Could not add files to stage";
                LOGGER.error(msg, e);
                System.out.println(msg);
                System.exit(1);
            }
        });

    }
}
