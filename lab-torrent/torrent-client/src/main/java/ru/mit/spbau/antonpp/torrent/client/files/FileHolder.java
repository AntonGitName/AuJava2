package ru.mit.spbau.antonpp.torrent.client.files;

import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.PartNotDownloadedException;
import ru.mit.spbau.antonpp.torrent.client.exceptions.SerializationException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
final class FileHolder implements FileSerializable {

    private static final int PART_SIZE = 4 * 1024;
    private static final String FILE_METADATA = "index.txt";

    private String path;
    private Map<Integer, Integer> parts = new HashMap<>();

    private static Path getPartPath(String path, int num) {
        return Paths.get(path, Integer.toString(num));
    }

    byte[] getPart(int num) throws IOException {
        if (!parts.containsKey(num)) {
            throw new PartNotDownloadedException();
        }
        return Files.readAllBytes(getPartPath(path, num));
    }

    static FileHolder create(Path pathToFile, String destination) throws IOException {
        val allBytes = Files.readAllBytes(pathToFile);
        val holder = new FileHolder();
        holder.path = destination;
        for (int i = 0; i < allBytes.length; i += PART_SIZE) {
            val data = Arrays.copyOfRange(allBytes, i, Math.min(i + PART_SIZE, allBytes.length));
            val part = i / PART_SIZE;
            holder.addPart(part, data);
        }

        holder.serialize(Paths.get(destination));
        return holder;
    }

    @Override
    public void serialize(Path path) {
        final Path index = path.resolve(FILE_METADATA);
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(index.toFile()))) {
            os.writeObject(path);
            os.writeObject(parts);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize ClientFileManager", e);
        }
    }

    @Override
    public void deserialize(Path path) {
        final Path index = path.resolve(FILE_METADATA);
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(index.toFile()))) {
            this.path = (String) os.readObject();
            parts = (Map<Integer, Integer>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    Set<Integer> getAvailableParts() {
        return parts.keySet();
    }

    public void addPart(int part, byte[] data) throws IOException {
        Files.write(getPartPath(path, part), data);
        parts.put(part, data.length);
    }

    public long getSize() {
        return parts.values().stream().mapToLong(x -> x).sum();
    }
}
