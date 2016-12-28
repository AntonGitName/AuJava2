package ru.mit.spbau.antonpp.torrent.client.ui.table;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.TorrentClient;
import ru.mit.spbau.antonpp.torrent.client.exceptions.FileManagerException;
import ru.mit.spbau.antonpp.torrent.client.files.FileHolder;
import ru.mit.spbau.antonpp.torrent.client.files.FileHolder.ClientFileRecord;
import ru.mit.spbau.antonpp.torrent.client.ui.data.Row;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * @author antonpp
 * @since 14/12/2016
 */
@Slf4j
public class FileViewTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {"Filename",
            "ID",
            "Full size",
            "Downloaded size",
            "Progress",
            "Blocks"
    };

    private final TorrentClient client;
    private final List<Row> records = new ArrayList<>();

    public FileViewTableModel(TorrentClient client) {
        this.client = client;
    }

    private static ProgressPanel createProgress(int fullSize, int downloadedSize) {
        return new ProgressPanel(fullSize, downloadedSize);
    }

    private BlockPanel createBlocks(int fullSize, Set<Integer> parts) {
        int numBlocks = (fullSize + FileHolder.BLOCK_SIZE - 1) / FileHolder.BLOCK_SIZE;
        return new BlockPanel(parts, numBlocks);
    }

    private Optional<Row> findRecord(int id) {
        return records.stream().filter(x -> x.getId() == id).findAny();
    }

    private void updateClientFile(ClientFileRecord record) {
        val id = record.getRealFile().getId();
        val opt = findRecord(id);
        val downloadedSize = (int) record.getDownloadedSize();
        try {
            if (opt.isPresent()) {
                val row = opt.get();
                row.setDownloadedSize(downloadedSize);
                row.getProgressPanel().updateDownloadedSize(downloadedSize);
                row.getBlockPanel().setBlocks(client.requestFilePartsList(id));
            } else {
                val real = record.getRealFile();
                val fullSize = (int) real.getSize();
                records.add(Row.builder()
                        .id(id)
                        .downloadedSize(downloadedSize)
                        .fullSize(real.getSize())
                        .name(real.getName())
                        .progressPanel(createProgress(fullSize, downloadedSize))
                        .blockPanel(createBlocks(fullSize, client.requestFilePartsList(id)))
                        .build());
            }
        } catch (FileManagerException e) {
            log.error("Failed to update file download progress", e);
        }
    }

    private void updateTrackerFile(TrackerFileRecord record) {
        val id = record.getId();
        val opt = findRecord(id);
        if (!opt.isPresent()) {
            val fullSize = (int) record.getSize();
            records.add(Row.builder()
                    .id(id)
                    .downloadedSize(0)
                    .fullSize(fullSize)
                    .name(record.getName())
                    .progressPanel(createProgress(fullSize, 0))
                    .blockPanel(createBlocks(fullSize, Collections.emptySet()))
                    .build()
            );
        }
    }

    public void update(Map<Integer, TrackerFileRecord> trackerFiles, List<ClientFileRecord> clientFiles) {
        trackerFiles.values().forEach(this::updateTrackerFile);
        clientFiles.forEach(this::updateClientFile);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 4:
            case 5:
                return JPanel.class;
            default:
                return super.getColumnClass(columnIndex);
        }
    }

    @Override
    public int getRowCount() {
        return records.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }


    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        val record = records.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return record.getName();
            case 1:
                return record.getId();
            case 2:
                return record.getFullSize();
            case 3:
                return record.getDownloadedSize();
            case 4:
                return record.getProgressPanel();
            case 5:
                return record.getBlockPanel();
            default:
                return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void updateClientFile(int id, long downloadedSize) {
        val opt = findRecord(id);
        if (opt.isPresent()) {
            opt.get().setDownloadedSize(downloadedSize);
            try {
                opt.get().getBlockPanel().setBlocks(client.requestFilePartsList(id));
            } catch (FileManagerException e) {
                log.error("Could not retrieve file blocks.", e);
            }
            opt.get().getProgressPanel().updateDownloadedSize((int) downloadedSize);
        } else {
            log.error("Received update request for unknown file");
        }
    }
}
