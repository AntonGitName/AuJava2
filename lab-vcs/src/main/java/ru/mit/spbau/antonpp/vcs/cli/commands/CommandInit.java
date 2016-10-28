package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.Repository;
import ru.mit.spbau.antonpp.vcs.core.branch.BranchResolver;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CommitException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.RepositoryLog;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
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

        if (root != null) {
            exitWithError(null, String.format("This folder has already an initialised repository at %s.", root));
        } else {
            try {
                Files.createDirectories(internals);
                Files.createDirectories(Utils.getStageFiles(currentDir));
                Files.createDirectories(Utils.getRevisionsDir(currentDir));

                final Stage stage = new Stage();
                stage.setRoot(currentDir);
                final String initialCommitHash = stage.commit();
                final Repository repository = new Repository();
                repository.setRoot(currentDir);
                repository.setHeadHash(initialCommitHash);
                stage.serialize(Utils.getStageIndex(currentDir));
                repository.serialize(Utils.getRepository(currentDir));
                new RepositoryLog().serialize(Utils.getLogFile(currentDir));
                new BranchResolver().serialize(Utils.getBranchesFile(currentDir));

                LOGGER.info("Commit {} created in {}", initialCommitHash, currentDir);
                System.out.println("Initial commit has been created");

            } catch (IOException | SerializationException | CommitException e) {
                exitWithError(e, "Failed to create repository in this folder.");
            }

        }
    }
}
