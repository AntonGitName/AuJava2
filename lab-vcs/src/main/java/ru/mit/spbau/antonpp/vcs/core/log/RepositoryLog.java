package ru.mit.spbau.antonpp.vcs.core.log;

import lombok.val;
import ru.mit.spbau.antonpp.vcs.core.FileSerializable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;
import ru.mit.spbau.antonpp.vcs.core.revision.Commit;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that holds log records.
 *
 * @author antonpp
 * @since 28/10/16
 */
public class RepositoryLog implements FileSerializable {

    private List<LogRecord> logRecords = new ArrayList<>();

    private static void loadAllParents(Path root, String hash, Set<String> hashes) throws SerializationException {
        val commit = new Commit();
        commit.deserialize(Utils.getRevisionIndex(root, hash));
        hashes.add(hash);
        for (String s : commit.getParents()) {
            loadAllParents(root, s, hashes);
        }
    }

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
     * Returns all records in the order from the newest to the oldest for specified commit
     *
     * @return list of records.
     */
    public List<LogRecord> getLogRecords(Path root, String hash) throws SerializationException {
        val parents = new HashSet<String>();
        loadAllParents(root, hash, parents);
        return getLogRecords().stream().filter(x -> parents.contains(x.getHash())).collect(Collectors.toList());
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
