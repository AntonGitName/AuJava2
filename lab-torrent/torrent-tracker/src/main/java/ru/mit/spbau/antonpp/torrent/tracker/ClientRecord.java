package ru.mit.spbau.antonpp.torrent.tracker;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Builder
public class ClientRecord {
    @Getter
    private final long lastUpdateTime;
    @Getter
    private final Set<Integer> files;
}
