package ru.mit.spbau.antonpp.vcs.core;

import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * @author antonpp
 * @since 27/10/16
 */
public interface FileSerializable extends Serializable {
    /**
     * @throws SerializationException in case of IOException during serialization
     */
    void serialize(Path path) throws SerializationException;

    /**
     * Replace current state with data from input path containing serialized data
     *
     * @throws SerializationException in case of IOException during deserialization
     */
    void deserialize(Path path) throws SerializationException;
}
