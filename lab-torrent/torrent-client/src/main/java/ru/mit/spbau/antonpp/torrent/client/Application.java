package ru.mit.spbau.antonpp.torrent.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.util.concurrent.FutureCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientStartException;
import ru.mit.spbau.antonpp.torrent.client.requests.ClientRequester.DownloadFileCallback;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;

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
    private static final String CMD_LIST = "connect";
    private static final String CMD_UPLOAD = "upload";
    private static final String CMD_DOWNLOAD = "download";

    private final JCommander jc;
    private TorrentClient torrentClient;

    @Parameter(names = {"--host"})
    private String host = "localhost";

    @Parameter(names = {"--client_port", "-p"})
    private short clientPort = 31001;

    @Parameter(names = {"--server_port"})
    private short serverPort = 8081;

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

    private static void printToLogAndSout(String msg, Throwable e) {
        log.error(msg, e);
        System.out.println("ERROR");
        System.out.println("Here is an exception message (check logs for more information):");
        System.out.println(msg);
    }

    private void run() {

        try {
            checkFirstStart();
            torrentClient = new TorrentClient(host, serverPort, clientPort, new UpdateCallback(), Paths.get(dir));
        } catch (TorrentClientStartException | IOException e) {
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
                    case CMD_HELP:
                    default:
                        printCommands();
                        break;
                }
            }
        }
    }

    private void handleListFiles() {
        final Map<Integer, FileRecord> records;
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
        final int id;
        final String pathToFile = args[1];
        try {
            id = torrentClient.requestUploadFile(pathToFile);
        } catch (RequestFailedException e) {
            printToLogAndSout("Failed to request list of files", e);
            return;
        }
        System.out.printf("`%s` was uploaded with id %d\n", pathToFile, id);
    }

    private void handleDownloadFile(String[] args) {
        val id = Integer.valueOf(args[1]);
        val destination = args[2];

        final SaveFileCallback callback = new SaveFileCallback();
        torrentClient.requestDownloadFileAsync(id, destination, callback);
        while (!callback.isReady()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                printToLogAndSout("Interrupted while downloading", e);
            }
        }
    }

    private void checkFirstStart() throws IOException {
        val workingDir = Paths.get(dir);
        if (Files.exists(workingDir)) {
            System.out.println("Found previously saved torrent files");
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
            printToLogAndSout("Could not send update info to tracker. Closing client...", t);
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
            System.out.printf("Progress: %.2f\n", (double) downloadedSize / fullSize);
        }
    }
}
