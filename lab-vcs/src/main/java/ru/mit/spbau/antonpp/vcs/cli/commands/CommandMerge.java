package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.val;
import ru.mit.spbau.antonpp.vcs.core.exceptions.CheckoutException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.MergeException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.LogRecord;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.util.List;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "merge", commandDescription = "Merge branches")
public class CommandMerge extends AbstractCommand {

    @Parameter(names = {"-m", "--messages"}, description = "specifies commit message")
    private String message;

    @Parameter(names = {"-a", "--author"}, description = "specifies author")
    private String author;

    @Parameter(required = true, description = "Name of the branch", arity = 1, validateValueWith = NoSpacesValidator.class)
    private List<String> name;

    public CommandMerge() {
        super(true);
    }

    @Override
    protected void runInternal() {
        if (author == null) {
            author = System.getProperty("user.name");
        }
        try {
            val info = LogRecord.builder().author(author).message(message).time(Utils.getCurrentTime()).build();
            repository.merge(name.get(0), info);
        } catch (MergeException | SerializationException | CheckoutException e) {
            exitWithError(e, "Failed to merge.");
        }
    }
}
