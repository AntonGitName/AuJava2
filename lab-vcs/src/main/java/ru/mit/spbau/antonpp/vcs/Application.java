package ru.mit.spbau.antonpp.vcs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.cli.commands.*;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private JCommander jc;

    @Parameter(names = { "-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    private final CommandAdd commandAdd = new CommandAdd();
    private final CommandBranch commandBranch = new CommandBranch();
    private final CommandCheckout commandCheckout = new CommandCheckout();
    private final CommandCommit commandCommit = new CommandCommit();
    private final CommandInit commandInit = new CommandInit();
    private final CommandMerge commandMerge = new CommandMerge();
    private final CommandStatus commandStatus = new CommandStatus();
    private final CommandReset commandReset = new CommandReset();
    private final CommandRemove commandRemove = new CommandRemove();
    private final CommandClean commandClean = new CommandClean();

    public static void main(String[] args) {
        Application app = new Application(args);
        // graphStats.run();
    }


    private Application(String[] args) {
        jc = new JCommander(this);

        jc.addCommand(commandAdd);
        jc.addCommand(commandBranch);
        jc.addCommand(commandCheckout);
        jc.addCommand(commandCommit);
        jc.addCommand(commandInit);
        jc.addCommand(commandMerge);
        jc.addCommand(commandStatus);

        try {
            jc.parse(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (help || jc.getParsedCommand() == null) {
            jc.usage();
            System.exit(0);
        }
    }


}
