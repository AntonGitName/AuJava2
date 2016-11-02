package ru.mit.spbau.antonpp.vcs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.cli.commands.*;

/**
 * This class is an entry point of the application. It is responsible only for parsing arguments.
 *
 * @author Anton Mordberg
 * @since 23.10.16
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

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
    private final CommandLog commandLog = new CommandLog();
    private final JCommander jc;

    @Parameter(names = {"-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    private Application(String[] args) {
        jc = new JCommander(this);

        jc.addCommand(commandAdd);
        jc.addCommand(commandBranch);
        jc.addCommand(commandCheckout);
        jc.addCommand(commandCommit);
        jc.addCommand(commandInit);
        jc.addCommand(commandMerge);
        jc.addCommand(commandStatus);
        jc.addCommand(commandReset);
        jc.addCommand(commandRemove);
        jc.addCommand(commandClean);
        jc.addCommand(commandLog);

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse input", e);
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (help || jc.getParsedCommand() == null) {
            jc.usage();
        } else {
            run();
        }
    }

    public static void main(String[] args) {
        new Application(args);
    }

    private void run() {
        final String command = jc.getParsedCommand();
        switch (command) {
            case "add":
                commandAdd.run();
                break;
            case "branch":
                commandBranch.run();
                break;
            case "checkout":
                commandCheckout.run();
                break;
            case "commit":
                commandCommit.run();
                break;
            case "getLogRecords":
                commandLog.run();
                break;
            case "init":
                commandInit.run();
                break;
            case "merge":
                commandMerge.run();
                break;
            case "status":
                commandStatus.run();
                break;
            case "reset":
                commandReset.run();
                break;
            case "rm":
                commandRemove.run();
                break;
            case "clean":
                commandClean.run();
                break;
            default:
                jc.usage();
        }
    }


}
