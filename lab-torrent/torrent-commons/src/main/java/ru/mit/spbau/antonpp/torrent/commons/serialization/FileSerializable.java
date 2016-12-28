package ru.mit.spbau.antonpp.torrent.commons.serialization;

import java.io.Serializable;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public interface FileSerializable extends Serializable {
    /**
     * Saves current state
     *
     * @throws SerializationException in case of IOException during serialization
     */
    void serialize();

    /**
     * Loads state
     *
     * @throws SerializationException in case of IOException during deserialization
     */
    void deserialize();
}