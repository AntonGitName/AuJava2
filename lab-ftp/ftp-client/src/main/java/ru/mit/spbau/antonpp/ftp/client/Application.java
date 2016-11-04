package ru.mit.spbau.antonpp.ftp.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.ftp.client.exceptions.FtpClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * This class is an entry point of the application. It is responsible for CLI.
 *
 * @author antonpp
 * @since 30/10/2016
 */
@Slf4j
public class Application {

    private final JCommander jc;
    private FtpClient ftpClient;

    @Parameter(names = {"--host"})
    private String host = "localhost";

    @Parameter(names = {"-p", "--port"})
    private int port = 12345;

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
        String fmt = "\t%s\t-\t%s%n";
        System.out.printf(fmt, "help", "Print list of available commands");
        System.out.printf(fmt, "exit", "Exit the application");
        System.out.printf(fmt, "get source destination", "Download specified file");
        System.out.printf(fmt, "list dir", "List contents of the dir");
        System.out.printf(fmt, "connect", "Connect to the server " +
                "(host and port were specified in command line arguments of the application)");
        System.out.printf(fmt, "disconnect", "Disconnect from the server");
    }

    public static void main(String[] args) throws IOException {
        new Application(args);
    }

    private void run() {

        readlineLoop();
        if (ftpClient != null) {
            handleDisconnect();
        }
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

                    case "connect":
                        handleConnect();
                        break;
                    case "disconnect":
                        handleDisconnect();
                        break;
                    case "exit":
                        System.out.println("Bye!");
                        return;
                    case "get":
                        handleGet(split);
                        break;
                    case "list":
                        handleList(split);
                        break;
                    case "help":
                    default:
                        printCommands();
                        break;
                }
            }
        }
    }

    private void handleDisconnect() {
        if (ftpClient == null) {
            System.out.println("You must connect first");
            return;
        }
        try {
            ftpClient.close();
            System.out.println("Disconnected from the server.");
        } catch (IOException e) {
            printToLogAndSout("Failed to correctly close client", e);
        } finally {
            ftpClient = null;
        }
    }

    private void handleConnect() {
        if (ftpClient != null) {
            System.out.println("You are already connected");
            return;
        }
        try {
            ftpClient = new FtpClient(host, port);
            System.out.println("Connected to the server.");
        } catch (FtpClientException e) {
            printToLogAndSout("Failed to start client " +
                    "(consider checking if FTP server is running on specified host:port)", e);
        }
    }

    private void handleList(String[] split) {
        if (ftpClient == null) {
            System.out.println("You must connect first");
            return;
        }
        if (split.length != 2) {
            System.out.println("You must provide exactly one path as an argument");
        } else {
            System.out.println("Waiting for response...");
            try {
                val response = ftpClient.listFiles(split[1]);
                response.entrySet().forEach(x -> System.out.printf("  %s%s\n", x.getKey(), x.getValue() ? "/" : ""));
            } catch (FtpClientException e) {
                printToLogAndSout("FTP client failed to execute LIST", e);
            }
        }
    }

    private void handleGet(String[] split) {
        if (ftpClient == null) {
            System.out.println("You must connect first");
            return;
        }
        if (split.length != 3) {
            System.out.println("You must provide path to the downloading file and where to save the result");
        } else {
            System.out.println("Waiting for response...");
            try {
                val response = ftpClient.getFile(split[1]);
                Files.write(Paths.get(split[2]), response);
                System.out.println("File saved");
            } catch (FtpClientException e) {
                printToLogAndSout("FTP client failed to execute GET", e);
            } catch (IOException e) {
                printToLogAndSout("Failed to save file at specified location", e);
            }
        }
    }

    private void printToLogAndSout(String msg, Exception e) {
        log.error(msg, e);
        System.out.println(msg);
    }
}
