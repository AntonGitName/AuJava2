package ru.mit.spbau.antonpp.ftp.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.ftp.server.exceptions.FtpServerException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;


/**
 * @author antonpp
 * @since 29/10/16
 */
@Slf4j
public class Application {

    private final JCommander jc;

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
        System.out.printf(fmt, "start", "Start FTP server on specified port (it is an argument for the application)");
        System.out.printf(fmt, "stop", "Stop FTP server");
    }

    public static void main(String[] args) throws IOException {
        new Application(args);
    }

    private void run() {

        val user = System.getProperty("user.name");
        val path = Paths.get(System.getProperty("user.home")).relativize(Paths.get(System.getProperty("user.dir")));

        val server = new FtpServer(port);

        boolean isServerStarted = false;

        try (Scanner scanner = new Scanner(System.in)) {

            while (true) {

                System.out.printf("%s@~/%s$ ", user, path);

                val line = scanner.nextLine();
                log.debug("User typed: {}", line);
                val split = line.split("\\s+");

                switch (split[0]) {

                    case "start":
                        try {
                            server.start();
                            isServerStarted = true;
                            System.out.println("Server started");
                        } catch (FtpServerException e) {
                            printToLogAndSout("Failed to start server", e);
                        }
                        break;

                    case "stop":
                        try {
                            server.stop();
                            isServerStarted = false;
                            System.out.println("Server stopped");
                        } catch (IOException e) {
                            printToLogAndSout("Failed to stop server", e);
                        }
                        break;

                    case "exit":
                        if (isServerStarted) {
                            try {
                                server.stop();
                            } catch (IOException e) {
                                printToLogAndSout("Failed to stop server", e);
                            }
                            System.out.println("Bye!");
                        }
                        return;

                    default:
                    case "help":
                        printCommands();
                        break;
                }
            }
        }

    }

    private void printToLogAndSout(String msg, Exception e) {
        log.error(msg, e);
        System.out.println(msg);
    }
}
