package ru.mit.spbau.antonpp.torrent.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientException;
import ru.mit.spbau.antonpp.torrent.client.files.ClientFileManager;

import java.io.IOException;
import java.nio.file.Files;
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
    private static final String CMD_LIST = "connect";
    private static final String CMD_UPLOAD = "upload";
    private static final String CMD_DOWNLOAD = "download";

    private final JCommander jc;
    private TorrentClient torrentClient;

    @Parameter(names = {"--host", "-h"})
    private String host = "localhost";

    @Parameter(names = {"--client_port", "-p"})
    private int clientPort = 10001;

    @Parameter(names = {"--server_port"})
    private int serverPort = 8081;

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
        System.out.printf(fmt, cmd, "Exit the application");
        cmd = String.format("%s <source> <name>", CMD_UPLOAD);
        System.out.printf(fmt, cmd, "Upload specified file");
        cmd = String.format("%s <name> <destination>", CMD_DOWNLOAD);
        System.out.printf(fmt, cmd, "Download specified file");
        cmd = CMD_LIST;
        System.out.printf(fmt, cmd, "List available files");
    }

    public static void main(String[] args) throws IOException {
        new Application(args);
    }

    private void run() {

        try {
            val fileManager = loadClientFiles();
            torrentClient = new TorrentClient(host, serverPort, clientPort, fileManager);
        } catch (TorrentClientException | IOException e) {
            printToLogAndSout("Could not start torrent client =(", e);
            return;
        }

        System.out.println("Client started.");

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
                val split = line.split("\\s+");

                switch (split[0]) {

                    case CMD_EXIT:
                        System.out.println("Bye!");
                        return;
                    case CMD_UPLOAD:
                        // TODO
                        break;
                    case CMD_DOWNLOAD:
                        // TODO
                        break;
                    case CMD_HELP:
                    default:
                        printCommands();
                        break;
                }
            }
        }
    }

    private ClientFileManager loadClientFiles() throws IOException {
        val fileManager = new ClientFileManager();
        val path = Paths.get(System.getProperty("user.dir"), ClientFileManager.MANAGER_DATA);
        if (Files.exists(path)) {
            System.out.println("Found previously saved torrent files");
            fileManager.deserialize(path);
        } else {
            System.out.println("No files saved in this dir. Creating a new storage.");
            Files.createDirectory(path);
            fileManager.serialize(path);
        }
        return fileManager;
    }

    private void printToLogAndSout(String msg, Exception e) {
        log.error(msg, e);
        System.out.println(msg);
    }
}
