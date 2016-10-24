package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "add", commandDescription = "Add file contents to the index")
public class CommandAdd {
    @Parameter(description = "Files to add to the index")
    private List<String> patterns;
}
