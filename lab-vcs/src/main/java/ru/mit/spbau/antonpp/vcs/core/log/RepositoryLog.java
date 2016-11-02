package ru.mit.spbau.antonpp.vcs.core.log;

import ru.mit.spbau.antonpp.vcs.core.FileSerializable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that holds log records.
 *
 * @author antonpp
 * @since 28/10/16
 */
public class RepositoryLog implements FileSerializable {

    private List<LogRecord> logRecords = new ArrayList<>();

    /**
     * Try to guess what it does.
     *
     * @param info record info.
     */
    public void addRecord(LogRecord info) {
        logRecords.add(info);
    }

    /**
     * Returns all records in the order from the newest to the oldest.
     *
     * @return list of records.
     */
    public List<LogRecord> getLogRecords() {
        return getLogRecords(logRecords.size());
    }

    /**
     * Returns {@code nRecords} newest records in the order from the newest to the oldest.
     *
     * @param nRecords number of records to return.
     * @return list of records.
     */
    public List<LogRecord> getLogRecords(int nRecords) {
        final int fromIndex = Math.max(logRecords.size() - nRecords, 0);
        final ArrayList<LogRecord> lastN = new ArrayList<>(logRecords.subList(fromIndex, logRecords.size()));
        Collections.reverse(lastN);
        return lastN;
    }

    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            os.writeObject(logRecords);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize getLogRecords", e);
        }
    }

    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            logRecords = (List<LogRecord>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }
}
