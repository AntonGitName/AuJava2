package ru.mit.spbau.antonpp.vcs.core.branch;

import org.jetbrains.annotations.Nullable;
import ru.mit.spbau.antonpp.vcs.core.FileSerializable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Class that manages repository branches.
 *
 * @author antonpp
 * @since 28/10/16
 */
public class BranchResolver implements FileSerializable {

    private Map<String, String> resolver = new HashMap<>();

    @Override
    public void serialize(Path path) throws SerializationException {
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            os.writeObject(resolver);
        } catch (IOException e) {
            throw new SerializationException("Could not serialize getLogRecords", e);
        }
    }

    @Override
    public void deserialize(Path path) throws SerializationException {
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            resolver = (Map<String, String>) os.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Could not deserialize revision", e);
        }
    }

    /**
     * Returns the latest commit in this branch.
     *
     * @param branch name of a branch.
     * @return full commit hash.
     */
    public String getBranchHead(String branch) {
        return resolver.get(branch);
    }

    /**
     * Opposite to {@link BranchResolver#getBranchHead(String)}.
     *
     * @param commitHash full commit hash.
     * @return name of the branch.
     */
    @Nullable
    public String findCommitBranch(String commitHash) {
        final Optional<String> branch = resolver.entrySet().stream().filter(x -> x.getValue().equals(commitHash))
                .map(Map.Entry::getKey).findFirst();
        return branch.isPresent() ? branch.get() : null;
    }

    /**
     * removes all records with specified branch.
     *
     * @param branch name of the branch to delete.
     */
    public void deleteBranch(String branch) {
        resolver.remove(branch);
    }

    /**
     * Updates branch with new latest commit.
     *
     * @param branch     name of a branch.
     * @param commitHash full hash of the commit.
     */
    public void updateBranch(String branch, String commitHash) {
        resolver.put(branch, commitHash);
    }

    /**
     * Checks if the specified branch has any records.
     *
     * @param branch name of the branch.
     * @return true if the specified branch has any records and false otherwise.
     */
    public boolean hasBranch(String branch) {
        return resolver.containsKey(branch);
    }

    /**
     * Returns set of all branches known to resolver.
     *
     * @return set of names.
     */
    public Set<String> getAllBranches() {
        return resolver.keySet();
    }
}
