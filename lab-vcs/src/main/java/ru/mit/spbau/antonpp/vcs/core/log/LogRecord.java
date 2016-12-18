package ru.mit.spbau.antonpp.vcs.core.log;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * This class represents an object that is normally stored in {@link RepositoryLog}.
 *
 * @author Anton Mordberg
 * @since 27.10.16
 */
@Data
@Builder
public class LogRecord implements Serializable {

    private String message;
    private String author;
    private String time;
    private String hash;

    public String getShortHash() {
        return getHash().substring(0, 6);
    }
}
