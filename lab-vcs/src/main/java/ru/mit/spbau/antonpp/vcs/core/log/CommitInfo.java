package ru.mit.spbau.antonpp.vcs.core.log;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * @author Anton Mordberg
 * @since 27.10.16
 */
public class CommitInfo implements Serializable {

    private String author;
    private String time;
    @Nullable
    private String msg;
    private String hash;

    public CommitInfo() {
    }

    public CommitInfo(String author, String time, @Nullable String msg) {
        this.author = author;
        this.time = time;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "CommitInfo{" +
                "author='" + author + '\'' +
                ", time='" + time + '\'' +
                ", msg='" + msg + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Nullable
    public String getMsg() {
        return msg;
    }

    public void setMsg(@Nullable String msg) {
        this.msg = msg;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getShortHash() {
        return hash.substring(0, 6);
    }
}
