package ru.mit.spbau.antonpp.vcs.core.status;

import ru.mit.spbau.antonpp.vcs.core.revision.Commit;
import ru.mit.spbau.antonpp.vcs.core.revision.Stage;
import ru.mit.spbau.antonpp.vcs.core.revision.WorkingDir;
import ru.mit.spbau.antonpp.vcs.core.utils.Utils;

import java.nio.file.Path;
import java.util.Map;

/**
 * @author Anton Mordberg
 * @since 26.10.16
 */
public class Status {

    private final RevisionDiff headDiff;
    private final RevisionDiff stageDiff;

    public Status(Commit head, Stage stage) {
        headDiff = new RevisionDiff(head, stage);
        stageDiff = new RevisionDiff(stage, new WorkingDir(stage.getRoot()));
    }

    public RevisionDiff getHeadDiff() {
        return headDiff;
    }

    public RevisionDiff getStageDiff() {
        return stageDiff;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
//        sb.append(String.format("On branch %s\n\n", stage.getBranch()));
        sb.append("Changes to be committed:\n\n");

        headDiff.getFiles().entrySet().stream()
                .filter(x -> x.getValue() != FileStatus.UNCHANGED)
                .forEach(x -> sb.append(String.format("\t%s\t%s\n", x.getValue().getShortName(), relative(x.getKey()))));

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

    private Path relative(Path fullPath) {
        return Utils.getCurrentDir().relativize(fullPath);
    }
}
