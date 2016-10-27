package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "branch", commandDescription = "Create a new branch")
public class CommandBranch extends CommandWithRepository {

    @Parameter(names = {"-d", "--delete"}, description = "Delete existing branch")
    private boolean toDelete;

    @Parameter(required = true, description = "Name of the branch", arity = 1)
    private List<String> name;

    @Override
    public void run() {
        super.run();
        System.out.println(name.get(0));
    }
}
