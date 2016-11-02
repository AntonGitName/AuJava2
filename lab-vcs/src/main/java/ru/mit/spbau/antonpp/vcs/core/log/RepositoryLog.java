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

    private List<CommitInfo> infos = new ArrayList<>();

    public void addRecord(CommitInfo info) {
        infos.add(info);
    }

    public List<CommitInfo> getLog() {
        return getLog(infos.size());
    }

    public List<CommitInfo> getLog(int nRecords) {
        final int fromIndex = Math.max(infos.size() - nRecords, 0);
        final ArrayList<CommitInfo> lastN = new ArrayList<>(infos.subList(fromIndex, infos.size()));
        Collections.reverse(lastN);
        return lastN;
    }

    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            os.writeObject(infos);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize getLogRecords", e);
        }
    }

    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            infos = (List<CommitInfo>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }
}
