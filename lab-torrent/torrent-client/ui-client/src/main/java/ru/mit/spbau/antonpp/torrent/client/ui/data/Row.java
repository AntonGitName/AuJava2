package ru.mit.spbau.antonpp.torrent.client.ui.data;

import lombok.Builder;
import lombok.Data;
import ru.mit.spbau.antonpp.torrent.client.ui.table.BlockPanel;
import ru.mit.spbau.antonpp.torrent.client.ui.table.ProgressPanel;

/**
 * @author antonpp
 * @since 14/12/2016
 */
@Data
@Builder
public final class Row {
    private final String name;
    private final int id;
    private final long fullSize;
    private final ProgressPanel progressPanel;
    private final BlockPanel blockPanel;
    private long downloadedSize;

}
