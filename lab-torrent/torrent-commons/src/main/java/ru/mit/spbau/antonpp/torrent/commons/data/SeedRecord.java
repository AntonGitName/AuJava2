package ru.mit.spbau.antonpp.torrent.commons.data;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@Data
@Builder
public final class SeedRecord implements Serializable {
    private byte[] ip;
    private short port;
}