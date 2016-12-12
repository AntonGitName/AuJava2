package ru.mit.spbau.antonpp.torrent.protocol.data;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author antonpp
 * @since 12/12/2016
 */
@ToString
@Builder
@EqualsAndHashCode
public final class SeedRecord {
    @Getter
    private final byte[] ip;
    @Getter
    private final short port;
}