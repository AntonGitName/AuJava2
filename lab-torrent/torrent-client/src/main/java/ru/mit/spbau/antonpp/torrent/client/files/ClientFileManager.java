package ru.mit.spbau.antonpp.torrent.client.files;

import lombok.val;
import ru.mit.spbau.antonpp.torrent.protocol.serialization.FileSerializable;
import ru.mit.spbau.antonpp.torrent.protocol.serialization.SerializationException;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class ClientFileManager implements FileSerializable {

    public static final String MANAGER_DATA = "torrent-client-files";
    private static final String FILES_MAP = "metainfo";
    private static final String FILES_STORAGE = "files";

    private Map<Integer, String> pathsToFiles = new HashMap<>();

    public ClientFileManager() {
    }

    public void saveFile(Path path, int id) throws IOException {
        FileHolder.create(path, FILES_STORAGE + id);
        pathsToFiles.put(id, FILES_STORAGE + id);
    }

    public byte[] getFilePart(int id, int part) throws IOException {
        if (pathsToFiles.containsKey(id)) {
            FileHolder holder = new FileHolder();
            final String pathToFolder = pathsToFiles.get(id);
            holder.deserialize(Paths.get(pathToFolder));
            return holder.getPart(pathToFolder, part);
        }
        throw new NoSuchFileException("" + id);
    }

    public Collection<Integer> getAvailableFiles() {
        return pathsToFiles.keySet();
    }

    public Set<Integer> getAvailableParts(int id) {
        if (pathsToFiles.containsKey(id)) {
            FileHolder holder = new FileHolder();
            holder.deserialize(Paths.get(pathsToFiles.get(id)));
            return holder.getAvailableParts();
        }
        return Collections.emptySet();
    }

    public void updateFilePart(int id, int part, byte[] data) throws IOException {
        FileHolder holder = new FileHolder();
        final String pathToFolder = pathsToFiles.get(id);
        val path = Paths.get(pathToFolder);
        if (pathsToFiles.containsKey(id)) {
            holder.deserialize(path);
        }
        holder.addPart(part, data, pathToFolder);
        holder.serialize(path);
    }

    public long getSize(int id) {
        if (pathsToFiles.containsKey(id)) {
            FileHolder holder = new FileHolder();
            holder.deserialize(Paths.get(pathsToFiles.get(id)));
            return holder.getSize();
        }
        return 0;
    }

    @Override
    public void serialize(Path path) {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.resolve(FILES_MAP).toFile()))) {
            os.writeObject(pathsToFiles);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize ClientFileManager", e);
        }
    }

    @Override
    public void deserialize(Path path) {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            pathsToFiles = (Map<Integer, String>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }
}
