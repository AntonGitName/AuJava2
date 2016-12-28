package ru.mit.spbau.antonpp.torrent.client.console;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.util.concurrent.FutureCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.TorrentClient;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientStartException;
import ru.mit.spbau.antonpp.torrent.client.requests.DownloadFileCallback;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
@Slf4j
public class Application {

    private static final String CMD_HELP = "help";
    private static final String CMD_EXIT = "exit";
    private static final String CMD_LIST = "list";
    private static final String CMD_UPLOAD = "upload";
    private static final String CMD_DOWNLOAD = "download";
    private static final String CMD_LOCAL = "local";
    private final static String ERROR_TIP = " Please check that:\n" +
            "   1. Tracker is running.\n" +
            "   2. Client port is not used by any other application.";
    private final JCommander jc;
    private TorrentClient torrentClient;
    @Parameter(names = {"--host"})
    private String host = "localhost";
    @Parameter(names = {"--client_port", "-p"})
    private int clientPort = 31001;
    @Parameter(names = {"--server_port"})
    private int serverPort = 8081;
    @Parameter(names = {"--directory", "-d"})
    private String dir = "torrent-client-files";
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
        final String fmt = "\t%32s\t-\t%s%n";
        String cmd = CMD_HELP;
        System.out.printf(fmt, cmd, "Print list of available commands");
        cmd = CMD_EXIT;
        System.out.printf(fmt, cmd, "Exit the application");
        cmd = String.format("%s <source> <id>", CMD_UPLOAD);
        System.out.printf(fmt, cmd, "Upload specified file");
        cmd = String.format("%s <id> <destination>", CMD_DOWNLOAD);
        System.out.printf(fmt, cmd, "Download specified file");
        cmd = CMD_LIST;
        System.out.printf(fmt, cmd, "List files known by tracker");
        cmd = CMD_LOCAL;
        System.out.printf(fmt, cmd, "List local uploaded files");
    }

    public static void main(String[] args) throws IOException {
        new Application(args);
    }

    private static void printToLogAndSout(String msg, Throwable e) {
        log.error(msg, e);
        System.out.printf("%s\n", msg);
        val cause = e.getCause();
        val tip = cause != null ? cause.toString() : e.getMessage();
        System.out.printf("%s (check logs for more)\n", tip);
    }

    private void run() {

        try {
            checkFirstStart();
            torrentClient = new TorrentClient(host, (short) serverPort, (short) clientPort, new UpdateCallback(), Paths.get(dir));
        } catch (TorrentClientStartException | IOException e) {
            printToLogAndSout("Could not start torrent client.", e);
            System.out.println(ERROR_TIP);
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
                        try {
                            torrentClient.close();
                        } catch (IOException e) {
                            printToLogAndSout("Could not save tracker state", e);
                        }
                        System.out.println("Bye!");
                        return;
                    case CMD_LIST:
                        handleListFiles();
                        break;
                    case CMD_UPLOAD:
                        handleUploadFile(split);
                        break;
                    case CMD_DOWNLOAD:
                        handleDownloadFile(split);
                        break;
                    case CMD_LOCAL:
                        handleLocal();
                        break;
                    case CMD_HELP:
                    default:
                        printCommands();
                        break;
                }
            }
        }
    }

    private void handleLocal() {
        final String fmt = "%20s\t%6d\t%10d\t%10d\t%3.2f\n";
        val records = torrentClient.requestLocalFiles();
        System.out.printf("%20s\t%6s\t%10s\t%10s\t%s\n", "Name", "ID", "Downloaded", "Full size", "Percent");
        records.forEach(record -> {
                    final TrackerFileRecord realFile = record.getRealFile();
                    System.out.printf(fmt, realFile.getName(), realFile.getId(), record.getDownloadedSize(),
                            realFile.getSize(), 100.0 * record.getRation());
                }
        );
    }

    private void handleListFiles() {
        final Map<Integer, TrackerFileRecord> records;
        final String fmt = "%20s\t%6d\t%d\n";
        try {
            records = torrentClient.requestFilesList();
        } catch (RequestFailedException e) {
            printToLogAndSout("Failed to request list of files", e);
            return;
        }
        System.out.printf("%20s\t%6s\t%s\n", "Filename", "ID", "Size");
        records.forEach((id, record) ->
                System.out.printf(fmt, record.getName(), id, record.getSize())
        );
    }

    private void handleUploadFile(String[] args) {
        final TrackerFileRecord record;
        final String pathToFile = args[1];
        final String name = args[2];
        try {
            record = torrentClient.requestUploadFile(pathToFile, name);
        } catch (RequestFailedException e) {
            printToLogAndSout("Failed to upload file.", e);
            return;
        }
        System.out.printf("%s was uploaded.\n", record);
    }

    private void handleDownloadFile(String[] args) {
        final int id;
        try {
            id = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("ID must be an integer.");
            return;
        }
        val destination = args[2];

        final SaveFileCallback callback = new SaveFileCallback();
        try {
            torrentClient.requestDownloadFile(id, destination, callback);
        } catch (RequestFailedException e) {
            printToLogAndSout("Could not download file.", e);
        }
    }

    private void checkFirstStart() throws IOException {
        val workingDir = Paths.get(dir);
        if (Files.exists(workingDir)) {
            System.out.println("Found previously saved torrent files.");
        } else {
            System.out.println("No files saved in this workingDir. Creating a new storage.");
            Files.createDirectory(workingDir);
        }
    }

    private final class UpdateCallback implements FutureCallback<Object> {
        @Override
        public void onSuccess(Object result) {
            log.info("Send update info to tracker successfully");
        }

        @Override
        public void onFailure(Throwable t) {
            printToLogAndSout("Client update request failed. Closing client...", t);
            System.out.println(ERROR_TIP);
            try {
                if (torrentClient != null) {
                    torrentClient.close();
                }
            } catch (IOException e) {
                printToLogAndSout("Could not close client", e);
            }
            System.exit(1);
        }
    }

    private final class SaveFileCallback implements DownloadFileCallback {

        @Getter
        private boolean ready = false;

        @Override
        public void onFinish(int id) {
            System.out.println("Download has finished.");
            ready = true;
        }

        @Override
        public void onFail(int id, Throwable e) {
            ready = true;
            printToLogAndSout(String.format("Download if file with id=%d has failed\n", id), e);
        }

        @Override
        public void progress(int id, long downloadedSize, long fullSize) {
            System.out.printf("Progress: %3.2f%%\n", 100.0 * downloadedSize / fullSize);
        }

        @Override
        public void noSeeds(int id) {
            System.out.println("Download has been stopped because " +
                    "there are not enough available parts of the file at the moment.");
        }
    }
}
