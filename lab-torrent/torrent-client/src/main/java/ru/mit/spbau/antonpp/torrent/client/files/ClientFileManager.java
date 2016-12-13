package ru.mit.spbau.antonpp.torrent.client.files;

import lombok.val;
import ru.mit.spbau.antonpp.torrent.commons.serialization.FileSerializable;
import ru.mit.spbau.antonpp.torrent.commons.serialization.SerializationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class ClientFileManager implements FileSerializable {

    private Path metainfoLocation;
    private Path filesLocation;

    private Map<Integer, FileHolder> files = new HashMap<>();

    private ClientFileManager() {
    }

    public static ClientFileManager load(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        val manager = new ClientFileManager();
        manager.metainfoLocation = dir.resolve("manager-metainfo");
        manager.filesLocation = dir.resolve("files");
        if (Files.exists(manager.metainfoLocation)) {
            manager.deserialize();
        } else {
            Files.createDirectory(manager.filesLocation);
            manager.serialize();
        }
        return manager;
    }

    public synchronized void saveFile(Path source, int id) throws IOException {
        files.put(id, FileHolder.create(source, filesLocation, id));
    }

    public synchronized byte[] getFilePart(int id, int part) throws IOException {
        if (files.containsKey(id)) {
            val holder = FileHolder.load(filesLocation, id);
            return holder.getPart(part);
        }
        throw new NoSuchFileException("" + id);
    }

    public synchronized Set<Integer> getAvailableFiles() {
        return Collections.unmodifiableSet(files.keySet());
    }

    public synchronized Set<Integer> getAvailableParts(int id) throws IOException {
        if (files.containsKey(id)) {
            return Collections.unmodifiableSet(files.get(id).getAvailableParts());
        }
        return Collections.emptySet();
    }

    public synchronized void updateFilePart(int id, int part, byte[] data) throws IOException {
        final FileHolder holder;
        if (!files.containsKey(id)) {
            files.put(id, FileHolder.createEmpty(filesLocation, id));
        }
        holder = files.get(id);
        holder.addPart(part, data);
        holder.serialize();
    }

    public synchronized long getSize(int id) throws IOException {
        if (files.containsKey(id)) {
            return files.get(id).getSize();
        }
        return 0;
    }

    @Override
    public void serialize() {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(metainfoLocation.toFile()))) {
            os.writeObject(new HashSet<>(files.keySet()));
        } catch (IOException e) {
            throw new SerializationException("Could not serialize ClientFileManager", e);
        }
    }

    @Override
    public void deserialize() {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(metainfoLocation.toFile()))) {
            Set<Integer> ids = (Set<Integer>) os.readObject();
            for (final Integer id : ids) {
                files.put(id, FileHolder.load(filesLocation, id));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize ClientFileManager", e);
        }
    }

    public void getFile(Path destination, int id) throws IOException {
        files.get(id).copyFile(destination);
    }
}
