package ru.mit.spbau.antonpp.vcs.core.log;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * This class represents an object that is normally stored in {@link RepositoryLog}.
 *
 * @author Anton Mordberg
 * @since 27.10.16
 */
@ToString
public class LogRecord implements Serializable {

    @Getter @Setter
    private String message;
    @Getter @Setter
    private String author;
    @Getter @Setter
    private String time;
    @Getter @Setter
    private String hash;

    public LogRecord() {
    }

    @Builder
    public LogRecord(String author, String time, String message) {
        this.author = author;
        this.time = time;
        this.message = message;
    }

    public String getShortHash() {
        return getHash().substring(0, 6);
    }
}
