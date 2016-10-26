package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameters;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "init", commandDescription = "Create index")
public class CommandInit {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInit.class);

    public void run() {
        final Path workingDir = Utils.getCurrentDir();
        final Path internals = Utils.getInternals(workingDir);

        LOGGER.debug("Internals path: {}", internals);

        final Path rootDir = Utils.getWorkingDir();
        if (rootDir != null) {
            final String msg = String.format("This folder has already an initialised repository at %s.", rootDir);
            LOGGER.warn(msg);
            System.out.println(msg);
            System.exit(1);
        } else {
            try {
                Files.createDirectories(internals);
                Files.createDirectories(Utils.getStageFiles(workingDir));
                Files.createDirectories(Utils.getRevisionsDir(workingDir));

                final String initialCommitHash = Hashing.md5().hashInt(239).toString();
                // rev/files
                Files.createDirectories(Utils.getRevisionFiles(workingDir, initialCommitHash));
                // rev/parents.txt
                Files.write(Utils.getRevisionParents(workingDir, initialCommitHash), initialCommitHash.getBytes());
                // rev/index.txt
                Files.createFile(Utils.getRevisionIndex(workingDir, initialCommitHash));

                // stage/index.txt
                Files.createFile(Utils.getStageIndex(workingDir));
                // stage/files
                Files.createDirectories(Utils.getStageFiles(workingDir));

                // /head.txt
                Files.write(Utils.getHeadHashFile(workingDir), initialCommitHash.getBytes());

                LOGGER.info("Commit {} created in {}", initialCommitHash, workingDir);

                System.out.println("Initial commit has been created");

            } catch (IOException e) {
                final String msg = "Failed to create repository in this folder.";
                LOGGER.error(msg, e);
                System.out.println(msg);
                System.exit(1);
            }

        }
    }
}
