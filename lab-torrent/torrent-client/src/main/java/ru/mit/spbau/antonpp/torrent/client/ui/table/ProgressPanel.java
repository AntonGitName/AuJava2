package ru.mit.spbau.antonpp.torrent.client.ui.table;

import javax.swing.*;
import java.awt.*;

/**
 * @author antonpp
 * @since 14/12/2016
 */
public class ProgressPanel extends JPanel {

    private final JProgressBar progressBar;

    public ProgressPanel(int fullSize, int downloadedSize) {
        setLayout(new BorderLayout(0, 1));

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, fullSize);
        progressBar.setValue(downloadedSize);
        progressBar.setStringPainted(true);

        add(progressBar);
    }

    public void updateDownloadedSize(int newSize) {
        progressBar.setValue(newSize);
    }
}
