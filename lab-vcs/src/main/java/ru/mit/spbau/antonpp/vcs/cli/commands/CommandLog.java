package ru.mit.spbau.antonpp.vcs.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anton Mordberg
 * @since 23.10.16
 */
@Parameters(commandNames = "log", commandDescription = "Show vcs log")
public class CommandLog extends CommandWithRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLog.class);

    @Parameter(names = {"-m", "--messages"}, description = "print records with commit message")
    private boolean message;

    @Parameter(names = {"-a", "--author"}, description = "print records with commit author")
    private boolean author;

    @Parameter(names = {"-t", "--time"}, description = "print records with commit time")
    private boolean time;

    @Override
    public void run() {
        super.run();
        final Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
        final List<String> logFileContent;
        try {
            logFileContent = Files.readAllLines(Utils.getLogFile(workingDir));
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
            for (final String line : logFileContent) {
                final Matcher matcher = pattern.matcher(line);
                final List<String> splittedLine = new ArrayList<>();
                while (matcher.find()) {
                    splittedLine.add(matcher.group(1).replace("\"", ""));
                }
                // LOGGER.debug("log line: {}", splittedLine);
                final String t = splittedLine.get(0);
                final String a = splittedLine.get(1);
                final String h = splittedLine.get(2);
                final String m = splittedLine.get(3);
                if (time) {
                    System.out.printf("%18s\t", t);
                }
                if (author) {
                    System.out.printf("%10s\t", a);
                }
                System.out.printf("%6s\t", h);
                if (message) {
                    System.out.printf("%s\t", m);
                }
                System.out.println();
                System.out.println();
            }
        } catch (IOException e) {
            final String msg = "Failed to read log.";
            LOGGER.error(msg, e);
            System.out.println(msg);
            System.exit(1);
        }
    }

}
