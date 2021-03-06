package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.log.LogRecord;

import java.util.List;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "log", commandDescription = "Show vcs log")
public class CommandLog extends AbstractCommand {

    @Parameter(names = {"-m", "--messages"}, description = "print records with commit message")
    private boolean message;

    @Parameter(names = {"-a", "--author"}, description = "print records with commit author")
    private boolean author;

    @Parameter(names = {"-t", "--time"}, description = "print records with commit time")
    private boolean time;

    public CommandLog() {
        super(true);
    }

    @Override
    public void runInternal() {
        final List<LogRecord> logFileContent;
        try {
            logFileContent = repository.getLogRecords();
            if (logFileContent.isEmpty()) {
                System.out.println("No records yet.");
            } else {
                printHeader();
                logFileContent.forEach(this::printLine);
            }
        } catch (SerializationException e) {
            exitWithError(e, "Failed to read getLogRecords.");
        }
    }

    private void printHeader() {
        if (time) {
            System.out.printf("%9s%-9s\t", " ", "time");
        }
        if (author) {
            System.out.printf("%-10s\t", "author");
        }
        System.out.printf("%-6s\t", " hash");
        if (message) {
            System.out.printf("%s\t", "   message");
        }
        System.out.println();
        System.out.println();
    }

    private void printLine(LogRecord info) {
        if (time) {
            System.out.printf("%18s\t", info.getTime());
        }
        if (author) {
            System.out.printf("%-10s\t", info.getAuthor());
        }
        System.out.printf("%-6s\t", info.getShortHash());
        if (message) {
            System.out.printf("%s\t", info.getMessage());
        }
        System.out.println();
        System.out.println();
    }

}
