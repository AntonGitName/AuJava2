package ru.mit.spbau.antonpp.torrent.client.files;

import lombok.val;
import ru.mit.spbau.antonpp.torrent.client.exceptions.FileManagerException;
import ru.mit.spbau.antonpp.torrent.client.files.FileHolder.LocalFileRecord;
import ru.mit.spbau.antonpp.torrent.commons.data.FileRecord;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Anton Mordberg
 * @since 11.12.16
 */
public class ClientFileManager {

    private final Path filesLocation;

    private final Map<Integer, FileHolder> files = new HashMap<>();

    public ClientFileManager(Path dir) throws IOException {
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        filesLocation = dir.resolve("files");
        if (Files.notExists(filesLocation)) {
            Files.createDirectory(filesLocation);
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(filesLocation)) {
            for (Path path : directoryStream) {
                val name = path.getFileName().toString();
                if (name.startsWith(FileHolder.FILE_PREFIX)) {
                    val id = Integer.valueOf(name.substring(FileHolder.FILE_PREFIX.length()));
                    files.put(id, FileHolder.load(filesLocation, id));
                }
            }
        }
    }

    public synchronized void saveFile(Path source, FileRecord record) throws IOException {
        files.put(record.getId(), FileHolder.create(source, filesLocation, record));
    }

    public synchronized byte[] getFilePart(int id, int part) throws IOException {
        if (hasFile(id)) {
            return files.get(id).getPart(part);
        }
        throw new FileManagerException("No file with id=" + id);
    }

    public synchronized Set<Integer> getAvailableFiles() {
        return Collections.unmodifiableSet(files.keySet());
    }

    public synchronized Set<Integer> getAvailableParts(int id) {
        if (hasFile(id)) {
            return Collections.unmodifiableSet(files.get(id).getAvailableParts());
        }
        throw new FileManagerException("No file with id=" + id);
    }

    public synchronized void createEmpty(FileRecord record) throws IOException {
        files.put(record.getId(), FileHolder.createEmpty(filesLocation, record));
    }

    public synchronized void updateFilePart(int id, int part, byte[] data) throws IOException {
        val holder = files.get(id);
        holder.addPart(part, data);
        holder.serialize();
    }

    public synchronized long getSize(int id) {
        if (hasFile(id)) {
            return files.get(id).getSize();
        }
        throw new FileManagerException("No file with id=" + id);
    }

    public void getFile(Path destination, int id) throws IOException {
        files.get(id).copyFile(destination);
    }

    public boolean hasFile(int id) {
        return files.containsKey(id);
    }

    public List<LocalFileRecord> getLocalRecords() {
        return files.values().stream().map(FileHolder::getRecord).collect(Collectors.toList());
    }
}
