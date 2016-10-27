package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.exceptions.StatusReadingException;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "branch", commandDescription = "Create a new branch")
public class CommandBranch extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBranch.class);

    @Parameter(names = {"-d", "--delete"}, description = "Delete existing branch")
    private boolean toDelete;

    @Parameter(required = true, description = "Name of the branch", arity = 1, validateValueWith = NoSpacesValidator.class)
    private List<String> name;

    private static final class NoSpacesValidator implements IValueValidator<String> {

        @Override
        public void validate(String name, String value) throws ParameterException {
            if (value.split(" ").length > 1) {
                throw new ParameterException("Branch name must be without spaces.");
            }
        }
    }

    @Override
    public void run() {
        super.run();
        LOGGER.debug("User specified branch: {}", name.get(0));
        final Stage stage = repository.getStage();
        if (!toDelete) {
            try {
                if (!stage.isClear() && stage.getBranch() != null) {
                    System.out.println("You must commit changes before switching to another branch");
                    System.exit(1);
                } else {
                    stage.setBranch(name.get(0));
                }
            } catch (StatusReadingException | IOException e) {
                LOGGER.error("", e);
                System.out.println("Failed to read stage status. Cannot switch to another branch");
                System.exit(1);

            }
        } else {
            final Map<String, String> branches;
            try {
                branches = Utils.readBranches(workingDir);
                if (!branches.containsKey(name.get(0))) {
                    System.out.println("No such branch");
                } else {
                    branches.remove(name.get(0));
                    Utils.writeBranches(branches, workingDir);

                }
            } catch (IOException e) {
                LOGGER.error("", e);
                System.out.println("Failed to read branches. Cannot delete branch.");
                System.exit(1);
            }
        }
    }
}
