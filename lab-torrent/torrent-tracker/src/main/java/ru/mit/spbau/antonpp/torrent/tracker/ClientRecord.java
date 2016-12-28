package ru.mit.spbau.antonpp.torrent.tracker;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Data
@Builder
public class ClientRecord implements Serializable {
    private long lastUpdateTime;
    private Set<Integer> files;
}
