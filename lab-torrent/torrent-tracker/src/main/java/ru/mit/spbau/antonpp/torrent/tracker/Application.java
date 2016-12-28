package ru.mit.spbau.antonpp.torrent.tracker;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.commons.Util;
import ru.mit.spbau.antonpp.torrent.tracker.exceptions.TrackerStartException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class Application {
    private static final String CMD_HELP = "help";
    private static final String CMD_EXIT = "exit";
    private static final String CMD_USERS = "users";
    private static final String CMD_FILES = "files";
    private static final Path TRACKER_FILES = Paths.get("./tracker-data");

    private final JCommander jc;
    private TorrentTracker tracker;

    @Parameter(names = {"-p", "--port"})
    private int port = 8081;

    @Parameter(names = {"-h", "--help"}, description = "Print this help message and exit", help = true)
    private boolean help;

    private Application(String[] args) throws IOException {
        jc = new JCommander(this);

        try {
            jc.parse(args);
        } catch (Exception e) {
            log.warn("Failed to parse input", e);
            System.out.println(e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (help) {
            jc.usage();
        } else {
            run();
        }
    }

    private static void printCommands() {
        final String fmt = "\t%s\t-\t%s%n";
        String cmd = CMD_HELP;
        System.out.printf(fmt, cmd, "Print list of available commands");
        cmd = CMD_EXIT;
        System.out.printf(fmt, cmd, "Exit the application and saves tracker state");
        cmd = CMD_USERS;
        System.out.printf(fmt, cmd, "List recently updated users");
        cmd = CMD_FILES;
        System.out.printf(fmt, cmd, "List available files");
    }

    public static void main(String[] args) throws IOException {
        new Application(args);
    }

    private static void printToLogAndSout(String msg, Throwable e) {
        log.error(msg, e);
        System.out.println("ERROR");
        System.out.println("Here is an exception message (check logs for more information):");
        System.out.println(e);
    }

    private void run() {

        try {
            if (Files.exists(TRACKER_FILES)) {
                System.out.println("Loading tracker state from disk...");
            } else {
                System.out.println("Could not found tracker state in current folder. " +
                        "Creating empty tracker...");
            }
            tracker = TorrentTracker.create(TRACKER_FILES, (short) port);
        } catch (TrackerStartException e) {
            printToLogAndSout("Failed to start tracker.", e);
            return;
        }

        System.out.println("Tracker started.");

        readlineLoop();
    }

    private void readlineLoop() {
        val user = System.getProperty("user.name");
        val path = Paths.get(System.getProperty("user.home")).relativize(Paths.get(System.getProperty("user.dir")));

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.printf("%s@~/%s$ ", user, path);

                val line = scanner.nextLine();
                log.debug("User typed: {}", line);

                switch (line) {

                    case CMD_FILES:
                        handleListFiles();
                        break;
                    case CMD_USERS:
                        handleListUsers();
                        break;
                    case CMD_EXIT:
                        try {
                            tracker.close();
                        } catch (IOException e) {
                            printToLogAndSout("Could not save tracker state", e);
                        }
                        System.out.println("Bye!");
                        return;
                    case CMD_HELP:
                    default:
                        printCommands();
                        break;
                }
            }
        }
    }

    private void handleListFiles() {
        tracker.getFiles().forEach(record -> System.out.printf("%s%n", record));
    }

    private void handleListUsers() {
        tracker.getUsers().forEach(x -> System.out.printf("ip=%s port=%d%n", Util.ipToStr(x.getIp()), x.getPort()));
    }
}
