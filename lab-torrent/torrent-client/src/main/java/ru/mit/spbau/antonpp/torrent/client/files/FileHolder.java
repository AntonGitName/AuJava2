package ru.mit.spbau.antonpp.torrent.client.files;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.InvalidBlockException;
import ru.mit.spbau.antonpp.torrent.commons.data.TrackerFileRecord;
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
public final class FileHolder implements FileSerializable {

    public static final String FILE_PREFIX = "file_";
    public static final int BLOCK_SIZE = 4 * 1024;
    private Map<Integer, Integer> blocks = new HashMap<>();
    @Getter
    private ClientFileRecord record;
    private Path fileDir;

    private FileHolder() {
    }

    synchronized static FileHolder load(Path workingDir, int id) throws IOException {
        val holder = new FileHolder();
        holder.fileDir = getFileFolder(workingDir, id);
        holder.deserialize();
        return holder;
    }

    synchronized static FileHolder create(Path source, Path workingDir, TrackerFileRecord record) throws IOException {
        val holder = createEmpty(workingDir, record);
        val allBytes = Files.readAllBytes(source);
        for (int i = 0; i < allBytes.length; i += BLOCK_SIZE) {
            val data = Arrays.copyOfRange(allBytes, i, Math.min(i + BLOCK_SIZE, allBytes.length));
            val part = i / BLOCK_SIZE;
            holder.addPart(part, data);
        }
        holder.serialize();
        return holder;
    }

    private static Path getFileFolder(Path workingDir, int id) {
        return workingDir.resolve(FILE_PREFIX + id);
    }

    static FileHolder createEmpty(Path workingDir, TrackerFileRecord record) throws IOException {
        val holder = new FileHolder();
        holder.record = ClientFileRecord.builder().downloadedSize(0).realFile(record).build();
        holder.fileDir = getFileFolder(workingDir, record.getId());
        Files.createDirectories(holder.fileDir);
        holder.serialize();
        return holder;
    }

    private Path getPartPath(int num) {
        return fileDir.resolve("part_" + num);
    }

    synchronized byte[] getPart(int num) throws IOException {
        if (!hasBlock(num)) {
            throw new InvalidBlockException("no such block");
        }
        return Files.readAllBytes(getPartPath(num));
    }

    @Override
    public void serialize() {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(getFileMeta().toFile()))) {
            os.writeObject(record);
            os.writeObject(blocks);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize FileHolder", e);
        }
    }

    @Override
    public void deserialize() {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(getFileMeta().toFile()))) {
            record = (ClientFileRecord) os.readObject();
            blocks = (Map<Integer, Integer>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize FileHolder", e);
        }
    }

    synchronized Set<Integer> getAvailableParts() {
        return blocks.keySet();
    }

    synchronized void addPart(int num, byte[] data) throws IOException {
        if (hasBlock(num)) {
            throw new InvalidBlockException("Block is already saved");
        }
        Files.write(getPartPath(num), data);
        record.addSize(data.length);
        blocks.put(num, data.length);
        serialize();
    }

    private boolean hasBlock(int num) {
        return blocks.containsKey(num);
    }

    long getSize() {
        return record.getDownloadedSize();
    }

    private Path getFileMeta() {
        return fileDir.resolve("metainfo");
    }

    synchronized void copyFile(Path destination) throws IOException {
        val parts = this.blocks.keySet().stream().sorted().collect(Collectors.toList());
        Files.write(destination, Files.readAllBytes(getPartPath(0)));
        for (int num = 1; num < parts.size(); num++) {
            Files.write(destination, Files.readAllBytes(getPartPath(num)), StandardOpenOption.APPEND);
        }
    }

    @Data
    @Builder
    public static final class ClientFileRecord implements Serializable {
        private final TrackerFileRecord realFile;
        private long downloadedSize;

        public void addSize(long sz) {
            downloadedSize += sz;
        }

        public double getRation() {
            return (double) downloadedSize / realFile.getSize();
        }
    }
}
