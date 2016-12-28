package ru.mit.spbau.antonpp.torrent.client.ui;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.TorrentClient;
import ru.mit.spbau.antonpp.torrent.client.exceptions.RequestFailedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.TorrentClientStartException;
import ru.mit.spbau.antonpp.torrent.client.files.FileHolder.ClientFileRecord;
import ru.mit.spbau.antonpp.torrent.client.requests.DownloadFileCallback;
import ru.mit.spbau.antonpp.torrent.client.ui.table.FileViewTableModel;
import ru.mit.spbau.antonpp.torrent.client.ui.table.ProgressCellRenderer;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

/**
 * @author antonpp
 * @since 14/12/2016
 */
@Slf4j
public class Application extends JFrame {

    private static final String MENU_FILE = "File";
    private static final String MENU_ITEN_UPLOAD = "Upload";
    private static final String MENU_ITEN_DOWNLOAD = "Download";
    private static final String MENU_ITEM_EXIT = "Exit";

    private final static String ERROR_TIP = "<html>Please check that:<br>" +
            "   1. Tracker is running.<br>" +
            "   2. Client port is not used by any other application.</html>";
    private final ExecutorService uploader = Executors.newSingleThreadExecutor();
    @Getter
    private TorrentClient client;
    private FileViewTableModel tableModel;
    private volatile boolean clientUsed = false;
    private Path workingDir;
    private InfiniteProgressPanel uploadProgress;

    public Application() {
        super("Torrent Client v-1.0");

        this.setBounds(100, 100, 740, 580);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                if (client != null) {
                    try {
                        client.close();
                        log.info("Client closed successfully");
                        client = null;
                    } catch (IOException e) {
                        log.error("Could not close client", e);
                    }
                }
            }
        });

        createMenu();

        initClient();

        setVisible(true);

        createFilesView();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Application::new);
    }

    private void exitOnUnrecoverableError(String msg, Throwable e) {
        clientUsed = true;
        log.error(msg, e);
        final JComponent[] message = new JComponent[]{
                new JLabel("Problem:"),
                new JLabel(msg),
                new JLabel("Message:"),
                new JLabel(e.getMessage()),
                new JLabel(ERROR_TIP)
        };

        JOptionPane.showMessageDialog(this, message, "Unrecoverable Error", JOptionPane.ERROR_MESSAGE);
        quit();
    }

    private void handleRecoverableError(String msg, Throwable e) {
        log.error(msg, e);
        val cause = e.getCause();
        final JComponent[] message = new JComponent[]{
                new JLabel("Problem:"),
                new JLabel(msg),
                new JLabel("Message:"),
                new JLabel((cause != null ? cause + " " : "") + e.getMessage())
        };

        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }


    private void createMenu() {
        val menuBar = new JMenuBar();
        val fileMenu = new JMenu(MENU_FILE);

        val exitItem = new JMenuItem(MENU_ITEM_EXIT);
        val uploadItem = new JMenuItem(MENU_ITEN_UPLOAD);
        val downloadItem = new JMenuItem(MENU_ITEN_DOWNLOAD);

        exitItem.addActionListener(e -> quit());
        uploadItem.addActionListener(e -> upload());
        downloadItem.addActionListener(e -> download());

        exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', SHIFT_DOWN_MASK));
        uploadItem.setAccelerator(KeyStroke.getKeyStroke('U', SHIFT_DOWN_MASK));
        downloadItem.setAccelerator(KeyStroke.getKeyStroke('D', SHIFT_DOWN_MASK));

        fileMenu.add(uploadItem);
        fileMenu.add(downloadItem);
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void download() {
        val fc = new JFileChooser(workingDir.toAbsolutePath().getParent().toFile());
        int returnVal = fc.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            val file = fc.getSelectedFile();

            Object[] ids;
            try {
                ids = client.requestFilesList().keySet().stream().toArray();

                if (ids.length == 0) {
                    JOptionPane.showMessageDialog(this, "No files are available at the moment");
                    return;
                }

                int id = (Integer) JOptionPane.showInputDialog(this, "Choose file id", "Download",
                        JOptionPane.QUESTION_MESSAGE, null, ids, ids[0]);
                final Path tmpVersion = file.toPath().resolveSibling(file.getName() + "-torrent");
                Files.createFile(tmpVersion);

                client.requestDownloadFile(id, file.getAbsolutePath(), new DownloadCallback(tmpVersion));
            } catch (RequestFailedException | IOException e) {
                handleRecoverableError("Could not download file", e);
            }

        }
    }

    private void updateDownloadProgress(int id, long downloadedSize) {
        tableModel.updateClientFile(id, downloadedSize);
        revalidate();
        repaint();
    }

    private void upload() {
        val fc = new JFileChooser(workingDir.toAbsolutePath().getParent().toFile());
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            val file = fc.getSelectedFile();
            uploadProgress.start();
            clientUsed = true;
            uploader.submit(() -> {
                try {
                    client.requestUploadFile(file.getAbsolutePath(), file.getName());
                    SwingUtilities.invokeLater(this::onUploadFinish);
                } catch (RequestFailedException e) {
                    SwingUtilities.invokeLater(() -> handleRecoverableError("Could not upload file.", e));
                }
            });
        }
    }

    private void onUploadFinish() {
        clientUsed = false;
        uploadProgress.stop();
        updateFilesView();
    }

    private void quit() {
        uploader.shutdownNow();
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void updateFilesView() {
        if (!clientUsed) {
            final Map<Integer, TrackerFileRecord> allFiles;
            final List<ClientFileRecord> localFiles;
            try {
                allFiles = client.requestFilesList();
                localFiles = client.requestLocalFiles();
            } catch (RequestFailedException e) {
                handleRecoverableError("List request failed", e);
                return;
            }
            tableModel.update(allFiles, localFiles);
            revalidate();
        }
    }

    private void createFilesView() {
        uploadProgress = new InfiniteProgressPanel("Uploading a file. Please wait...");
        setGlassPane(uploadProgress);


        tableModel = new FileViewTableModel(client);

        val table = new JTable(tableModel);
        table.setDefaultRenderer(JPanel.class, new ProgressCellRenderer());
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(20);
        table.getColumnModel().getColumn(2).setPreferredWidth(20);
        table.getColumnModel().getColumn(3).setPreferredWidth(20);
        table.getColumnModel().getColumn(4).setPreferredWidth(20);
        table.getColumnModel().getColumn(5).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        updateFilesView();

        table.setRowHeight(table.getRowHeight() * 2);

        val updateTimer = new Timer(1000, e -> updateFilesView());
        updateTimer.setInitialDelay(2000);
        updateTimer.start();
    }

    private void initClient() {
        JTextField dir = new JTextField("client-files");
        JTextField host = new JTextField("localhost");
        JTextField tp = new JTextField("8081");
        JTextField cp = new JTextField("31001");
        final JComponent[] inputs = new JComponent[]{
                new JLabel("Working directory"),
                dir,
                new JLabel("Host"),
                host,
                new JLabel("Tracker port"),
                tp,
                new JLabel("Client port"),
                cp
        };
        int cPort;
        int tPort;

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, inputs, "Client settings", JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                workingDir = Paths.get(dir.getText());
                try {
                    cPort = Integer.valueOf(cp.getText());
                    tPort = Integer.valueOf(tp.getText());
                    break;
                } catch (NumberFormatException e) {
                    handleRecoverableError("Could not parse ports", e);
                }
            } else {
                quit();
            }
        }

        try {
            client = new TorrentClient(host.getText(), tPort, cPort, new ClientUpdateCallback(), workingDir);
        } catch (TorrentClientStartException e) {
            exitOnUnrecoverableError("Could not start client.", e);
        }
    }

    private final class DownloadCallback implements DownloadFileCallback {

        private final Path toDelete;

        private DownloadCallback(Path toDelete) {
            this.toDelete = toDelete;
        }

        private void deleteTmpFile() {
            try {
                Files.delete(toDelete);
            } catch (IOException e) {
                log.error("Could not delete tmp file", e);
            }
        }

        @Override
        public void onFinish(int id) {
            deleteTmpFile();
            JOptionPane.showMessageDialog(Application.this, "Downloading of file with id=" + id + " has finished");
        }

        @Override
        public void onFail(int id, Throwable e) {
            deleteTmpFile();
            handleRecoverableError("Downloading of file with id=" + id + " failed", e);
        }

        @Override
        public void progress(int id, long downloadedSize, long fullSize) {
            deleteTmpFile();
            updateDownloadProgress(id, downloadedSize);
        }

        @Override
        public void noSeeds(int id) {
            deleteTmpFile();
            JOptionPane.showMessageDialog(Application.this, "Downloading of file with id=" + id +
                    " was stopped because there is no seeds at the moment");
        }
    }

    private final class ClientUpdateCallback implements FutureCallback<Object> {

        @Override
        public void onSuccess(Object result) {
            log.info("Send update info to tracker successfully");
        }

        @Override
        public void onFailure(Throwable t) {
            exitOnUnrecoverableError("Client update request failed.", t);
        }
    }
}
