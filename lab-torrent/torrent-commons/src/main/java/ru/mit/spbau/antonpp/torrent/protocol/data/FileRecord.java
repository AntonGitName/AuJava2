package ru.mit.spbau.antonpp.torrent.protocol.data;

import lombok.Builder;
import lombok.Getter;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Builder
public final class FileRecord {
    @Getter
    private final String name;
    @Getter
    private final long size;
}
