package ru.mit.spbau.antonpp.vcs.core.log;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
@ToString
public class CommitInfo implements Serializable {

    @Getter
    @Setter
    private String msg;
    @Getter
    @Setter
    private String author;
    @Getter
    @Setter
    private String time;
    @Getter
    @Setter
    private String hash;

    public CommitInfo() {
    }

    @Builder
    public CommitInfo(String author, String time, String msg, String hash) {
        this.author = author;
        this.time = time;
        this.msg = msg;
        this.hash = hash;
    }

    public String getShortHash() {
        return getHash().substring(0, 6);
    }
}
