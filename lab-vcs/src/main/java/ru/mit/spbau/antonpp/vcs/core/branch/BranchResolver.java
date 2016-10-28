package ru.mit.spbau.antonpp.vcs.core.branch;

import ru.mit.spbau.antonpp.vcs.core.FileSerializable;
import ru.mit.spbau.antonpp.vcs.core.exceptions.BranchException;
import ru.mit.spbau.antonpp.vcs.core.exceptions.SerializationException;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
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
            throw new SerializationException("Could not serialize log", e);
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

    public String getBranchHead(String branch) throws BranchException {
        if (!resolver.containsKey(branch)) {
            throw new BranchException("No such branch");
        }
        return resolver.get(branch);
    }

    public Optional<String> findCommitBranch(String commitHash) {
        return resolver.entrySet().stream().filter(x -> x.getValue().equals(commitHash)).map(Map.Entry::getKey)
                .findFirst();
    }

    public void deleteBranch(String branch) throws BranchException {
        if (!resolver.containsKey(branch)) {
            throw new BranchException("No such branch");
        }
        resolver.remove(branch);
    }

    public void updateBranch(String branch, String comitHash) {
        resolver.put(branch, comitHash);
    }

    public boolean hasBranch(String branch) {
        return resolver.containsKey(branch);
    }
}
