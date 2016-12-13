package ru.mit.spbau.antonpp.torrent.client.files;

import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.PartNotDownloadedException;
import ru.mit.spbau.antonpp.torrent.commons.serialization.FileSerializable;
import ru.mit.spbau.antonpp.torrent.commons.serialization.SerializationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
final class FileHolder implements FileSerializable {

    private static final int PART_SIZE = 4 * 1024;

    private Map<Integer, Integer> parts = new HashMap<>();
    private int id;
    private Path fileDir;

    private FileHolder() {
    }

    synchronized static FileHolder load(Path workingDir, int id) throws IOException {
        val holder = new FileHolder();
        holder.fileDir = getFileFolder(workingDir, id);
        holder.id = id;
        holder.deserialize();
        return holder;
    }

    synchronized static FileHolder create(Path source, Path workingDir, int id) throws IOException {
        val holder = createEmpty(workingDir, id);
        val allBytes = Files.readAllBytes(source);
        for (int i = 0; i < allBytes.length; i += PART_SIZE) {
            val data = Arrays.copyOfRange(allBytes, i, Math.min(i + PART_SIZE, allBytes.length));
            val part = i / PART_SIZE;
            holder.addPart(part, data);
        }
        holder.serialize();
        return holder;
    }

    private static Path getFileFolder(Path workingDir, int id) {
        return workingDir.resolve("file_" + id);
    }

    static FileHolder createEmpty(Path workingDir, int id) throws IOException {
        val holder = new FileHolder();
        holder.fileDir = getFileFolder(workingDir, id);
        holder.id = id;
        Files.createDirectories(holder.fileDir);
        return holder;
    }

    private Path getPartPath(int num) {
        return fileDir.resolve("part_" + num);
    }

    synchronized byte[] getPart(int num) throws IOException {
        if (!parts.containsKey(num)) {
            throw new PartNotDownloadedException();
        }
        return Files.readAllBytes(getPartPath(num));
    }

    @Override
    public void serialize() {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(getFileMeta().toFile()))) {
            os.writeObject(parts);
            os.writeInt(id);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize FileHolder", e);
        }
    }

    @Override
    public void deserialize() {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(getFileMeta().toFile()))) {
            parts = (Map<Integer, Integer>) os.readObject();
            id = os.readInt();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize FileHolder", e);
        }
    }

    synchronized Set<Integer> getAvailableParts() {
        return parts.keySet();
    }

    synchronized void addPart(int part, byte[] data) throws IOException {
        Files.write(getPartPath(part), data);
        parts.put(part, data.length);
    }

    synchronized long getSize() {
        return parts.values().stream().mapToLong(x -> x).sum();
    }

    private Path getFileMeta() {
        return fileDir.resolve("metainfo");
    }

    synchronized void copyFile(Path destination) throws IOException {
        val parts = this.parts.keySet().stream().sorted().collect(Collectors.toList());
        Files.write(destination, Files.readAllBytes(getPartPath(0)));
        for (int num = 1; num < parts.size(); num++) {
            Files.write(destination, Files.readAllBytes(getPartPath(num)), StandardOpenOption.APPEND);
        }
    }
}
