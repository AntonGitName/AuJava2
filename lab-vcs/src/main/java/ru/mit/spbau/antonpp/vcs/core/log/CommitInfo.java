package ru.mit.spbau.antonpp.vcs.core.log;

import ru.mit.spbau.antonpp.vcs.core.exceptions.LogWriteException;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class CommitInfo {

    private final String author;
    private final String date;
    private final String msg;
    private final String shortHash;

    public CommitInfo(String author, String date, String msg, String shortHash) {
        this.author = author;
        this.date = date;
        this.msg = msg;
        this.shortHash = shortHash;
    }

    public void saveToLog(Path workingDir) throws LogWriteException {
        final byte[] record = String.format("\"%s\" \"%s\" \"%s\" \"%s\"\n", date, author, shortHash, msg).getBytes();
        try {
            Files.write(Utils.getLogFile(workingDir), record, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new LogWriteException("Could not add a record to the log file", e);
        }
    }
}
