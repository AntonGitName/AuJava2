package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CommitException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "commit", commandDescription = "Record changes to the repository")
public class CommandCommit extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandCommit.class);

    @Parameter(names = {"-m", "--messages"}, description = "specifies commit message")
    private String message;

    @Parameter(names = {"-a", "--author"}, description = "specifies author")
    private String author;

    public CommandCommit() {
        super(true);
    }

    @Override
    public void runInternal() {
        if (author == null) {
            author = System.getProperty("user.name");
        }
        try {
            repository.commit(new CommitInfo(author, getCurrentTime(), message));
        } catch (CommitException | SerializationException e) {
            exitWithError(e, "Failed to commit changes.");
        }
    }

    private String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
