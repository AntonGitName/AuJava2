package ru.mit.spbau.antonpp.torrent.client.files;

import ru.mit.spbau.antonpp.torrent.client.exceptions.SerializationException;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public interface FileSerializable extends Serializable {
    /**
     * Saves current state with data to the specified file.
     *
     * @param path path to file.
     * @throws SerializationException in case of IOException during serialization
     */
    void serialize(Path path);

    /**
     * Replace current state with data from input path containing serialized data
     *
     * @param path path to file.
     * @throws SerializationException in case of IOException during deserialization
     */
    void deserialize(Path path);
}