package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "commit", commandDescription = "Record changes to the repository")
public class CommandCommit {

    @Parameter(names = {"-m", "--messages"}, description = "specifies commit message")
    private String message;
}
