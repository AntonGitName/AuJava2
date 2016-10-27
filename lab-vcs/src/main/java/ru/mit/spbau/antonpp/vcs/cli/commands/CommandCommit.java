package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.*;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;
import ru.mit.spbau.antonpp.vcs.core.revision.Status;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "commit", commandDescription = "Record changes to the repository")
public class CommandCommit extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandCommit.class);

    @Parameter(names = {"-m", "--messages"}, description = "specifies commit message")
    private String message;

    @Parameter(names = {"-a", "--author"}, description = "specifies author")
    private String author;

    @Override
    public void run() {
        super.run();
        if (message == null) {
            message = generateMessage();
        }
        if (author == null) {
            author = System.getProperty("user.name");
        }
        final String date = getCurrentTime();
        try {
            final String commitHash = repository.getStage().commit();
            repository.checkout(commitHash);
        } catch (StatusReadingException | CommitException | RevisionCheckoutException |
                CheckoutIOException | CheckoutStageNotClearException e) {
            final String msg = "Failed to commit changes.";
            LOGGER.error(msg, e);
            System.out.println(msg);
            System.exit(1);
        }
        final String shortHash = repository.getStage().getParentRevision().getShortHash();
        final CommitInfo commitInfo = new CommitInfo(author, date, message, shortHash);
        try {
            commitInfo.saveToLog(workingDir);
        } catch (LogWriteException e) {
            final String msg = "Failed to write log record.";
            LOGGER.error(msg, e);
            System.out.println(msg);
            System.exit(1);
        }
        System.out.println("All changes have been commited.");
        LOGGER.debug("New head at: {}", shortHash);
    }

    private String generateMessage() {
        try {
            final Status status = new Status(repository.getStage(), workingDir);
            final int added = status.getStageAdded().size();
            final int modified = status.getStageModified().size();
            final int removed = status.getStageRemoved().size();
            return String.format("Added: %d, Modified: %d, Removed: %d", added, modified, removed);
        } catch (StatusReadingException e) {
            return "Empty message";
        }
    }

    private String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
