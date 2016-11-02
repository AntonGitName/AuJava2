package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CommitException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.CommitInfo;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "commit", commandDescription = "Record changes to the repository")
public class CommandCommit extends AbstractCommand {

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
            final CommitInfo info = CommitInfo.builder().author(author).time(Utils.getCurrentTime()).msg(message).build();
            repository.commit(info);
        } catch (CommitException | SerializationException e) {
            exitWithError(e, "Failed to commit changes.");
        }
    }
}
