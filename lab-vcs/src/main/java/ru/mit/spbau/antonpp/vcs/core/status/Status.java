package ru.mit.spbau.antonpp.vcs.core.status;

import lombok.Getter;
import ru.mit.spbau.antonpp.vcs.core.revision.Revision;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.revision.WorkingDir;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Path;
import java.util.Map;

/**
 * This class wrappes information about comparison between three revisions. It is supposed to be used for vcs status
 * message generation (that's why revisions are called Head, Stage and WorkingDir).
 *
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Status {

    @Getter
    private final RevisionDiff headDiff;
    @Getter
    private final RevisionDiff stageDiff;
    private final String branch;
    private final String headHash;

    /**
     * Saves all the information about files modifications between three revisions. Second revision is always stage
     * because status needs information about current branch.
     *
     * @param head  HEAD.
     * @param stage stage.
     */
    public Status(Revision head, Stage stage) {
        this(head, stage, stage.getBranch(), stage.getRoot());
    }

    /**
     * This constructor can compare not only HEAD and Stage, but any types of revisions. This functionality is currently
     * unused and constructor is made private.
     *
     * @param head   first revision.
     * @param stage  second revision.
     * @param branch stages's branch.
     * @param root   repository root.
     */
    private Status(Revision head, Revision stage, String branch, Path root) {
        this.branch = branch;
        headDiff = new RevisionDiff(head, stage);
        stageDiff = new RevisionDiff(stage, new WorkingDir(root));
        headHash = head.getRevHash();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (branch != null) {
            sb.append(String.format("On branch %s%n%n", branch));
        } else {
            sb.append(String.format("HEAD AT %s%n%n", headHash));
        }
        sb.append("Changes to be committed:\n\n");

        headDiff.getFiles().entrySet().stream()
                .filter(x -> x.getValue() != FileStatus.UNCHANGED)
                .forEach(x -> sb.append(String.format("\t%s\t%s%n", x.getValue().getShortName(), relative(x.getKey()))));

        sb.append("\nChanges not staged for commit:\n\n");
        stageDiff.getFiles().entrySet().stream()
                .filter(x -> x.getValue() != FileStatus.ADDED)
                .filter(x -> x.getValue() != FileStatus.UNCHANGED)
                .forEach(x -> sb.append(String.format("\t%s\t%s\n", x.getValue().getShortName(), relative(x.getKey()))));
        sb.append("\nUntracked files:\n\n");
        stageDiff.getFiles().entrySet().stream().filter(x -> x.getValue() == FileStatus.ADDED).map(Map.Entry::getKey)
                .map(this::relative).forEach(x -> sb.append(String.format("\t\t%s\n", x)));
        return sb.toString();
    }

    /**
     * Creates relative to repository root path. Should be used only for files in repository.
     *
     * @param fullPath path to be shorten.
     * @return relative path.
     */
    private Path relative(Path fullPath) {
        return Utils.getCurrentDir().relativize(fullPath);
    }
}
