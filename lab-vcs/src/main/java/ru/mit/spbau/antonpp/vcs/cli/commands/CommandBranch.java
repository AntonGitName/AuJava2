package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.BranchException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.util.List;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "branch", commandDescription = "Create a new branch")
public class CommandBranch extends AbstractCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBranch.class);

    @Parameter(names = {"-d", "--delete"}, description = "Delete existing branch")
    private boolean toDelete;

    @Parameter(required = true, description = "Name of the branch", arity = 1, validateValueWith = NoSpacesValidator.class)
    private List<String> name;

    public CommandBranch() {
        super(true);
    }

    @Override
    public void runInternal() {
        final String branch = name.get(0);
        LOGGER.debug("User specified branch: {}", branch);
        try {
            if (toDelete) {
                repository.addBranch(branch);
            } else {
                repository.deleteBranch(branch);

            }
        } catch (SerializationException | BranchException e) {
            exitWithError(e, e.getMessage());

        }
    }

    private static final class NoSpacesValidator implements IValueValidator<String> {

        @Override
        public void validate(String name, String value) throws ParameterException {
            if (value.split(" ").length > 1) {
                throw new ParameterException("Branch name must be without spaces.");
            }
        }
    }
}
