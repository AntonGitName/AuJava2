package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "branch", commandDescription = "Create a new branch")
public class CommandBranch {

    @Parameter(names = {"-d", "--delete"}, description = "Deletes existing branch")
    private Boolean toDelete;

    @Parameter(required = true, description = "Name of the branch")
    private String name;

}
