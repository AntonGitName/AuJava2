package ru.mit.spbau.antonpp.vcs.core.log;

import ru.mit.spbau.antonpp.vcs.core.FileSerializable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author antonpp
 * @since 28/10/16
 */
public class RepositoryLog implements FileSerializable {

    private List<CommitInfo> logRecords = new ArrayList<>();

    public void addRecord(CommitInfo info) {
        logRecords.add(info);
    }

    public List<CommitInfo> getLogRecords() {
        return getLogRecords(logRecords.size());
    }

    public List<CommitInfo> getLogRecords(int nRecords) {
        final int fromIndex = Math.max(logRecords.size() - nRecords, 0);
        final ArrayList<CommitInfo> lastN = new ArrayList<>(logRecords.subList(fromIndex, logRecords.size()));
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
            logRecords = (List<CommitInfo>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }
}
